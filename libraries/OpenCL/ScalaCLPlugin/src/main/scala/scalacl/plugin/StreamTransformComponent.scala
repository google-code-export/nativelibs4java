/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalacl ; package plugin

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

object StreamTransformComponent {
  val runsAfter = List[String](
    "namer"
  )
  val runsBefore = List[String]("refchecks", LoopsTransformComponent.phaseName)
  val phaseName = "scalacl-stream"
}

class StreamTransformComponent(val global: Global, val options: ScalaCLPlugin.PluginOptions)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
   with TraversalOps
   with Streams 
   with StreamSources 
   with StreamOps
   with StreamSinks
   with WithOptions
   with WorkaroundsForOtherPhases
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees
  import System.getenv
  
  override val runsAfter = StreamTransformComponent.runsAfter
  override val runsBefore = StreamTransformComponent.runsBefore
  override val phaseName = StreamTransformComponent.phaseName

  def newTransformer(compilationUnit: CompilationUnit) = new TypingTransformer(compilationUnit) {

    class OpsStream(val source: StreamSource, val colTree: Tree, val ops: List[StreamTransformer])
    object OpsStream {
      def unapply(tree: Tree) = {
        var ops = List[StreamTransformer]()
        var colTree = tree
        var source: StreamSource = null
        var finished = false
        while (!finished) {
          //println("Trying to match " + colTree)
          colTree match {
            case TraversalOp(traversalOp) if traversalOp.op.isInstanceOf[StreamTransformer] =>
              //println("found op " + traversalOp + "\n\twith collection = " + traversalOp.collection)
              val trans = traversalOp.op.asInstanceOf[StreamTransformer]
              if (trans.resultKind != StreamResult)
                ops = List()
              ops = trans :: ops
              colTree = traversalOp.collection
            case StreamSource(cr) =>
              //println("found streamSource " + cr + " (ops = " + ops + ")")
              source = cr
              if (colTree != cr.unwrappedTree)
                colTree = cr.unwrappedTree
              else
                finished = true
            case _ =>
              //if (!ops.isEmpty)
              //  println("Finished with " + ops.size + " ops upon "+ tree + " ; source = " + source + " ; colTree = " + colTree)
              finished = true
          }
        }
        if (ops.isEmpty && source == null)
          None
        else
          Some(new OpsStream(source, colTree, ops))
      }
    }

    val unit = compilationUnit

    var matchedColTreeIds = Set[Long]()

    override def transform(tree: Tree): Tree = {
      if (!shouldOptimize(tree))
        super.transform(tree)
      else
        try {
          tree match {
            case ArrayTabulate(componentType, lengths @ (firstLength :: otherLengths), f @ Func(params, body), manifest) if "0" != getenv("SCALACL_TRANSFORM_TABULATE") =>
              val tpe = body.tpe
              val returnType = if (tpe.isInstanceOf[ConstantType]) 
                tpe.widen
              else
                tpe
              
              val lengthDefs = lengths.map(length => newVariable(unit, "n$", currentOwner, tree.pos, false, length.setType(IntClass.tpe)))
                  
              msg(unit, tree.pos, "transformed Array.tabulate[" + returnType + "] into equivalent while loop") {
                  
                def replaceTabulates(lengthDefs: List[VarDef], parentArrayIdentGen: IdentGen, params: List[ValDef], mappings: Map[Symbol, TreeGen], symbolReplacements: Map[Symbol, Symbol]): (Tree, Type) = {
              
                  val param = params.head
                  val pos = tree.pos
                  val nVar = lengthDefs.head
                  val iVar = newVariable(unit, "i$", currentOwner, pos, true, newInt(0))
                  val iVal = newVariable(unit, "i$val$", currentOwner, pos, false, iVar())
                  
                  val newMappings: Map[Symbol, TreeGen] = mappings + (param.symbol -> iVal)
                  val newReplacements = symbolReplacements ++ Map(param.symbol -> iVal.symbol, f.symbol -> currentOwner)
                  
                  val mappedArrayTpe = getArrayType(lengthDefs.size, returnType)
                  
                  val arrayVar = if (parentArrayIdentGen == null)
                    newVariable(unit, "m$", currentOwner, tree.pos, false, newArrayMulti(mappedArrayTpe, returnType, lengthDefs.map(_.identGen()), manifest))
                  else
                    VarDef(parentArrayIdentGen, null, null)
                  
                  val subArrayVar =  if (lengthDefs.tail == Nil)
                    null
                  else
                    newVariable(unit, "subArray$", currentOwner, tree.pos, false, newApply(tree.pos, arrayVar(), iVal()))
                                    
                  val (newBody, bodyType) = if (lengthDefs.tail == Nil)
                      (
                          replaceOccurrences(
                            body,
                            newMappings,
                            newReplacements,
                            Map(),
                            unit
                          ),
                          returnType
                      )
                  else
                      replaceTabulates(
                        lengthDefs.tail,
                        subArrayVar,
                        params.tail,
                        newMappings,
                        newReplacements
                      )
                  
                  newBody.setType(bodyType)
                  
                  (
                    super.transform {
                      typed {
                        treeCopy.Block(
                          tree,
                          (
                            if (parentArrayIdentGen == null) 
                              lengthDefs.map(_.definition) :+ arrayVar.definition
                            else 
                              Nil
                          ) ++
                          List(
                            iVar.definition,
                            whileLoop(
                              currentOwner,
                              unit,
                              tree,
                              binOp(
                                iVar(),
                                IntClass.tpe.member(nme.LT),
                                nVar()
                              ),
                              Block(
                                (
                                  if (lengthDefs.tail == Nil)
                                    List(
                                      iVal.definition,
                                      newUpdate(
                                        tree.pos,
                                        arrayVar(),
                                        iVar(),
                                        newBody
                                      )
                                    )
                                  else {
                                    List(
                                      iVal.definition,
                                      subArrayVar.definition,
                                      newBody
                                    )
                                  }
                                ),
                                incrementIntVar(iVar, newInt(1))
                              )
                            )
                          ),
                          if (parentArrayIdentGen == null)
                            arrayVar()
                          else
                            newUnit
                        )
                      }
                    },
                    mappedArrayTpe
                  )
                }
                replaceTabulates(lengthDefs, null, params, Map(), Map())._1
              }
            case OpsStream(opsStream) 
            if 
              options.stream &&
              (opsStream.source ne null) && 
              !opsStream.ops.isEmpty && 
              (opsStream ne null) && 
              (opsStream.colTree ne null) && 
              !matchedColTreeIds.contains(opsStream.colTree.id) 
              =>
              import opsStream._
              
              val txt = "Streamed ops on " + (if (source == null) "UNKNOWN COL" else source.tree.tpe) + " : " + ops/*.map(_.getClass.getName)*/.mkString(", ")
              matchedColTreeIds += colTree.id
              msg(unit, tree.pos, "# " + txt) {
                val canCreateSinkLookup = source +: ops
                val sinkCreatorOpt = 
                  if (ops.last.resultKind == StreamResult)
                    canCreateSinkLookup.collect({ case ccss: CanCreateStreamSink => ccss }).lastOption match {
                      case Some(sinkCreator) =>
                        Some(sinkCreator)
                      case _ =>
                        throw new UnsupportedOperationException("Failed to find any CanCreateStreamSink instance in source ++ ops = " + canCreateSinkLookup + " !")
                    }
                  else
                    None
                
                val asm = assembleStream(Stream(source, ops, sinkCreatorOpt), this.transform _, unit, tree.pos, currentOwner, localTyper)
                //println(txt + "\n\t" + asm.toString.replaceAll("\n", "\n\t"))
                asm
              }
            case _ =>
              super.transform(tree)//toMatch)
          }
        } catch {
          case ex =>
            //ex.printStackTrace
            super.transform(tree)
        }
    }
  }
}
