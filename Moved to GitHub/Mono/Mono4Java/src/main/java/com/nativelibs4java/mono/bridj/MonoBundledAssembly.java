package com.nativelibs4java.mono.bridj;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * <i>native declaration : mono/metadata/assembly.h</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("mono") 
public class MonoBundledAssembly extends StructObject {
	public MonoBundledAssembly() {
		super();
	}
	public MonoBundledAssembly(Pointer pointer) {
		super(pointer);
	}
	/// C type : const char*
	@Field(0) 
	public Pointer<Byte > name() {
		return this.io.getPointerField(this, 0);
	}
	/// C type : const char*
	@Field(0) 
	public MonoBundledAssembly name(Pointer<Byte > name) {
		this.io.setPointerField(this, 0, name);
		return this;
	}
	/// C type : const unsigned char*
	@Field(1) 
	public Pointer<Byte > data() {
		return this.io.getPointerField(this, 1);
	}
	/// C type : const unsigned char*
	@Field(1) 
	public MonoBundledAssembly data(Pointer<Byte > data) {
		this.io.setPointerField(this, 1, data);
		return this;
	}
	@Field(2) 
	public int size() {
		return this.io.getIntField(this, 2);
	}
	@Field(2) 
	public MonoBundledAssembly size(int size) {
		this.io.setIntField(this, 2, size);
		return this;
	}
}
