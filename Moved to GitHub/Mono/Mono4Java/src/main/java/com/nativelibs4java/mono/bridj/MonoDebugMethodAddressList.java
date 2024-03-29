package com.nativelibs4java.mono.bridj;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * <i>native declaration : /Library/Frameworks/Mono.framework/Headers/mono-2.0/mono/metadata/mono-debug.h:92</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("mono") 
public class MonoDebugMethodAddressList extends StructObject {
	public MonoDebugMethodAddressList() {
		super();
	}
	public MonoDebugMethodAddressList(Pointer pointer) {
		super(pointer);
	}
	@Field(0) 
	public int size() {
		return this.io.getIntField(this, 0);
	}
	@Field(0) 
	public MonoDebugMethodAddressList size(int size) {
		this.io.setIntField(this, 0, size);
		return this;
	}
	@Field(1) 
	public int count() {
		return this.io.getIntField(this, 1);
	}
	@Field(1) 
	public MonoDebugMethodAddressList count(int count) {
		this.io.setIntField(this, 1, count);
		return this;
	}
	/// C type : mono_byte[0]
	@Array({0}) 
	@Field(2) 
	public Pointer<Byte > data() {
		return this.io.getPointerField(this, 2);
	}
}
