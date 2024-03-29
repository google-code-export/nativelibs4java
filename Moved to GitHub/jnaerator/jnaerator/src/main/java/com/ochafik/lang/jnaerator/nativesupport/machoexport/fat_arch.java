package com.ochafik.lang.jnaerator.nativesupport.machoexport;
/**
 * <i>native declaration : sources/com/ochafik/lang/jnaerator/nativesupport/machoexport.h:20</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a>, <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class fat_arch extends com.ochafik.lang.jnaerator.runtime.Structure<fat_arch, fat_arch.ByValue, fat_arch.ByReference> {
	public int cputype;
	public int cpusubtype;
	public int offset;
	public int size;
	public int align;
	public fat_arch() {
		super();
	}
	public fat_arch(int cputype, int cpusubtype, int offset, int size, int align) {
		super();
		this.cputype = cputype;
		this.cpusubtype = cpusubtype;
		this.offset = offset;
		this.size = size;
		this.align = align;
	}
	protected ByReference newByReference() { return new ByReference(); }
	protected ByValue newByValue() { return new ByValue(); }
	protected fat_arch newInstance() { return new fat_arch(); }
	public static class ByReference extends fat_arch implements com.sun.jna.Structure.ByReference {}
	public static class ByValue extends fat_arch implements com.sun.jna.Structure.ByValue {}
}
