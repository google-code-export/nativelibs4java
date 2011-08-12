/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
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
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import static com.nativelibs4java.util.JNAUtils.toNS;
import static com.nativelibs4java.util.NIOUtils.directBytes;
import static com.nativelibs4java.util.NIOUtils.directCopy;

import java.nio.*;

import com.nativelibs4java.opencl.library.cl_buffer_region;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_event;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.util.listenable.Pair;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * OpenCL Memory Buffer Object.<br/>
 * A buffer object stores a one-dimensional collection of elements.<br/>
 * Elements of a buffer object can be a scalar data type (such as an int, float), vector data type, or a user-defined structure.<br/>
 * @see CLContext
 * @author Olivier Chafik
 */
public class CLBuffer<T> extends CLMem {
    /// Buffer to retain a reference to (because it's being copied asynchronously to the underlying OpenCL buffer)
	Buffer buffer;
    final int elementSize;
    final Class<? extends Buffer> typedBufferClass;
    final Class<T> elementClass;
	CLBuffer(CLContext context, long byteCount, cl_mem entity, Buffer buffer, int elementSize, Class<? extends Buffer> typedBufferClass) {
        super(context, byteCount, entity);
		this.buffer = buffer;
        this.elementSize = elementSize;
        this.typedBufferClass = typedBufferClass;
        this.elementClass = (Class)NIOUtils.getPrimitiveClass(typedBufferClass);
	}
    
    public Class<? extends Buffer> getBufferClass() {
        return typedBufferClass;
    }
	public Class<T> getElementClass() {
        return elementClass;
    }
	public int getElementSize() {
        return elementSize;
    }
	public long getElementCount() {
        return getByteCount() / getElementSize();
    }
	public <B extends Buffer> B map(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return (B)map(queue, flags, 0, getElementCount(), true, eventsToWaitFor).getFirst();
    }
	public <B extends Buffer> B map(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return (B)map(queue, flags, offset, length, true, eventsToWaitFor).getFirst();
    }
	public <B extends Buffer> Pair<B, CLEvent> mapLater(CLQueue queue, MapFlags flags, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, 0, getElementCount(), false, eventsToWaitFor);
    }
	public <B extends Buffer> Pair<B, CLEvent> mapLater(CLQueue queue, MapFlags flags, long offset, long length, CLEvent... eventsToWaitFor) throws CLException.MapFailure {
		return map(queue, flags, offset, length, false, eventsToWaitFor);
    }
	public <B extends Buffer> B read(CLQueue queue, CLEvent... eventsToWaitFor) {
        B out = (B)typedBuffer(directBytes((int)getByteCount(), queue.getDevice().getKernelsDefaultByteOrder()));
        read(queue, out, true, eventsToWaitFor);
		return out;
	}
	public <B extends Buffer> B read(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		B out = (B)typedBuffer(directBytes((int)getByteCount(), queue.getDevice().getKernelsDefaultByteOrder()));
        read(queue, offset, length, out, true, eventsToWaitFor);
		return out;
	}

	protected void checkBounds(long offset, long length) {
		if (offset + length * getElementSize() > getByteCount())
			throw new IndexOutOfBoundsException("Trying to map a region of memory object outside allocated range");
	}

	/**
	 * Can be used to create a new buffer object (referred to as a sub-buffer object) from an existing buffer object.
	 * @param usage is used to specify allocation and usage information about the image memory object being created and is described in table 5.3 of the OpenCL spec.
	 * @param offset
	 * @param length
	 * @since OpenCL 1.1
	 * @return sub-buffer that is a "window" of this buffer starting at the provided offset, with the provided length
	 */
	public CLBuffer<T> createSubBuffer(Usage usage, long offset, long length) {
		try {
			int s = getElementSize();
			cl_buffer_region region = new cl_buffer_region(toNS(s * offset), toNS(s * length));
			IntByReference pErr = new IntByReference();
	        cl_mem mem = CL.clCreateSubBuffer(getEntity(), usage.getIntFlags(), CL_BUFFER_CREATE_TYPE_REGION, region.getPointer(), pErr);
	        error(pErr.getValue());
	        return mem == null ? null : createBuffer(mem);
		} catch (Throwable th) {
    		// TODO check if supposed to handle OpenCL 1.1
    		throw new UnsupportedOperationException("Cannot create sub-buffer (OpenCL 1.1 feature).", th);
    	}
	}
	
	/**
	 * enqueues a command to copy a buffer object identified by src_buffer to another buffer object identified by destination.
	 * @param queue
	 * @param destination
	 * @param eventsToWaitFor
	 * @return event which indicates the copy operation has completed
	 */
	public CLEvent copyTo(CLQueue queue, CLMem destination, CLEvent... eventsToWaitFor) {
		return copyTo(queue, 0, getElementCount(), destination, 0, eventsToWaitFor);	
	}
	
	/**
	 * enqueues a command to copy a buffer object identified by src_buffer to another buffer object identified by destination.
	 * @param queue
	 * @param srcOffset
	 * @param length
	 * @param destination
	 * @param destOffset
	 * @param eventsToWaitFor
	 * @return event which indicates the copy operation has completed
	 */
	public CLEvent copyTo(CLQueue queue, long srcOffset, long length, CLMem destination, long destOffset, CLEvent... eventsToWaitFor) {
		cl_event[] eventOut = CLEvent.new_event_out(eventsToWaitFor);
		long 
			byteCount = getByteCount(),
			destByteCount = destination.getByteCount(),
			eltSize = getElementSize(),
			actualSrcOffset = srcOffset * eltSize, 
			actualDestOffset = destOffset * eltSize, 
			actualLength = length * eltSize;
		
		if (	actualSrcOffset < 0 ||
			actualSrcOffset >= byteCount ||
			actualSrcOffset + actualLength > byteCount ||
			actualDestOffset < 0 ||
			actualDestOffset >= destByteCount ||
			actualDestOffset + actualLength > destByteCount
		)
			throw new IndexOutOfBoundsException("Invalid copy parameters : srcOffset = " + srcOffset + ", destOffset = " + destOffset + ", length = " + length + " (element size = " + eltSize + ", source byte count = " + byteCount + ", destination byte count = " + destByteCount + ")"); 
		
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueCopyBuffer(
			queue.getEntity(),
			getEntity(),
			destination.getEntity(),
			toNS(actualSrcOffset),
			toNS(actualDestOffset),
			toNS(actualLength),
			evts == null ? 0 : evts.length, evts,
			eventOut
		));
		return CLEvent.createEvent(queue, eventOut);
	}

	protected <B extends Buffer> Pair<B, CLEvent> map(CLQueue queue, MapFlags flags, long offset, long length, boolean blocking, CLEvent... eventsToWaitFor) {
		checkBounds(offset, length);
		cl_event[] eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
		IntByReference pErr = new IntByReference();
        
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        Pointer p = CL.clEnqueueMapBuffer(queue.getEntity(), getEntity(), blocking ? CL_TRUE : CL_FALSE,
			flags.value(),
			toNS(offset * getElementSize()),
            toNS(length * getElementSize()),
			evts == null ? 0 : evts.length, evts,
			eventOut,
			pErr
		);
		error(pErr.getValue());
        return new Pair<B, CLEvent>(
			(B)typedBuffer(p.getByteBuffer(0, length * getElementSize()).order(queue.getDevice().getKernelsDefaultByteOrder())),
			CLEvent.createEvent(queue, eventOut)
		);
    }

    public <B extends Buffer> CLEvent unmap(CLQueue queue, B buffer, CLEvent... eventsToWaitFor) {
        typedBufferClass.cast(buffer);
        cl_event[] eventOut = CLEvent.new_event_out(eventsToWaitFor);
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueUnmapMemObject(queue.getEntity(), getEntity(), Native.getDirectBufferPointer(buffer), evts == null ? 0 : evts.length, evts, eventOut));
		return CLEvent.createEvent(queue, eventOut);
    }

	public <B extends Buffer> CLEvent read(CLQueue queue, B out, boolean blocking, CLEvent... eventsToWaitFor) {
        typedBufferClass.cast(out);
        long length;
        if (isGL) {
            length = out.capacity();
        } else {
            length = getElementCount();
            long s = out.capacity();
            if (length > s)
                length = s;
        }
		return read(queue, 0, length, out, blocking, eventsToWaitFor);
	}

	public CLEvent read(CLQueue queue, long offset, long length, Buffer out, boolean blocking, CLEvent... eventsToWaitFor) {
        typedBufferClass.cast(out);
        if (out.isReadOnly())
            throw new IllegalArgumentException("Output buffer for read operation is read-only !");
        Buffer originalOut = null;
        if (!out.isDirect()) {
            originalOut = out;
            out = typedBuffer(directBytes((int)(length * getElementSize()), queue.getDevice().getKernelsDefaultByteOrder()));
            blocking = true;
        }
        cl_event[] eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueReadBuffer(
            queue.getEntity(),
            getEntity(),
            blocking ? CL_TRUE : 0,
            toNS(offset * getElementSize()),
            toNS(length * getElementSize()),
            Native.getDirectBufferPointer(out),
            evts == null ? 0 : evts.length, evts,
            eventOut
        ));
        if (originalOut != null)
            NIOUtils.put(out, originalOut);
        return CLEvent.createEvent(queue, eventOut);
    }

	public CLEvent write(CLQueue queue, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
        typedBufferClass.cast(in);
        long length;
        if (isGL) {
            length = in.capacity();
        } else {
            length = getElementCount();
            long s = in.capacity();
            if (length > s)
                length = s;
        }
		return write(queue, 0, length, in, blocking, eventsToWaitFor);
	}

	public CLEvent write(CLQueue queue, long offset, long length, Buffer in, boolean blocking, CLEvent... eventsToWaitFor) {
        typedBufferClass.cast(in);
        
        if (!in.isDirect()) {
            blocking = true;
            in = typedBuffer(directCopy(in, queue.getDevice().getKernelsDefaultByteOrder()));
        }
            
        cl_event[] eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueWriteBuffer(
            queue.getEntity(),
            getEntity(),
            blocking ? CL_TRUE : CL_FALSE,
            toNS(offset * getElementSize()),
            toNS(length * getElementSize()),
            Native.getDirectBufferPointer(in),
            evts == null ? 0 : evts.length, evts,
            eventOut
        ));
        return CLEvent.createEvent(queue, eventOut);
    }

    public CLEvent writeBytes(CLQueue queue, long offset, long length, ByteBuffer in, boolean blocking, CLEvent... eventsToWaitFor) {
        if (!in.isDirect()) {
            blocking = true;
            in = directCopy(in, queue.getDevice().getKernelsDefaultByteOrder());
        }

        cl_event[] eventOut = blocking ? null : CLEvent.new_event_out(eventsToWaitFor);
        cl_event[] evts = CLEvent.to_cl_event_array(eventsToWaitFor);
        error(CL.clEnqueueWriteBuffer(
            queue.getEntity(),
            getEntity(),
            blocking ? CL_TRUE : 0,
            toNS(offset),
            toNS(length),
            Native.getDirectBufferPointer(in),
            evts == null ? 0 : evts.length, evts,
            eventOut
        ));
        return CLEvent.createEvent(queue, eventOut);
    }

	public ByteBuffer readBytes(CLQueue queue, long offset, long length, CLEvent... eventsToWaitFor) {
		ByteBuffer out = directBytes((int)getByteCount(), queue.getDevice().getKernelsDefaultByteOrder());
        Buffer tout = typedBuffer(out);
		read(queue, offset, tout.capacity(), tout, true, eventsToWaitFor);
		return out;
	}

    private <T extends CLMem> T copyGLMark(T mem) {
        mem.isGL = this.isGL;
        return mem;
    }
    
    public CLBuffer<T> emptyClone(CLMem.Usage usage) {
    		return (CLBuffer)getContext().createBuffer(usage, typedBufferClass, getElementCount());
    }
    
    protected CLBuffer<T> createBuffer(cl_mem mem) {
        
        //return new CLBuffer<T>(getContext(), -1, mem, null, getElementSize(), typedBufferClass);
        
        #foreach ($prim in $primitivesNoBool)
        
        if (typedBufferClass == ${prim.BufferName}.class)
            return (CLBuffer<T>)new CL${prim.BufferName}(getContext(), -1, mem, null);
            
        #end
                
        throw new RuntimeException("Unable to create an OpenCL buffer for type " + typedBufferClass.getName());
    }
    
    protected <B extends Buffer> B typedBuffer(ByteBuffer b) {
        #foreach ($prim in $primitivesNoBool)
        
        if (typedBufferClass == ${prim.BufferName}.class)
            #if ($prim.Name == "byte")
            return (B)b;
            #else
            return (B)b.as${prim.BufferName}();
            #end
    
        #end
                
        throw new RuntimeException("Unable to create a typed buffer for type " + typedBufferClass.getName());
    }

    #foreach ($prim in $primitivesNoBool)

	public CL${prim.BufferName} asCL${prim.BufferName}() {
		cl_mem mem = getEntity();
		CL.clRetainMemObject(getEntity());
		return copyGLMark(new CL${prim.BufferName}(context, getByteCount(), mem, buffer));
	}
	
	#end
    
}
