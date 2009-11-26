package com.nativelibs4java.mono.library;
/**
 * Keep in sync with OffsetTable in mcs/class/Mono.CSharp.Debugger/MonoSymbolTable.cs<br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a>, <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class MonoSymbolFileOffsetTable extends com.ochafik.lang.jnaerator.runtime.Structure<MonoSymbolFileOffsetTable, MonoSymbolFileOffsetTable.ByValue, MonoSymbolFileOffsetTable.ByReference> {
	public int _total_file_size;
	public int _data_section_offset;
	public int _data_section_size;
	public int _compile_unit_count;
	public int _compile_unit_table_offset;
	public int _compile_unit_table_size;
	public int _source_count;
	public int _source_table_offset;
	public int _source_table_size;
	public int _method_count;
	public int _method_table_offset;
	public int _method_table_size;
	public int _type_count;
	public int _anonymous_scope_count;
	public int _anonymous_scope_table_offset;
	public int _anonymous_scope_table_size;
	public int _line_number_table_line_base;
	public int _line_number_table_line_range;
	public int _line_number_table_opcode_base;
	public int _is_aspx_source;
	public MonoSymbolFileOffsetTable() {
		super();
	}
	protected ByReference newByReference() { return new ByReference(); }
	protected ByValue newByValue() { return new ByValue(); }
	protected MonoSymbolFileOffsetTable newInstance() { return new MonoSymbolFileOffsetTable(); }
	public static MonoSymbolFileOffsetTable[] newArray(int arrayLength) {
		return com.ochafik.lang.jnaerator.runtime.Structure.newArray(MonoSymbolFileOffsetTable.class, arrayLength);
	}
	public static class ByReference extends MonoSymbolFileOffsetTable implements com.sun.jna.Structure.ByReference {}
	public static class ByValue extends MonoSymbolFileOffsetTable implements com.sun.jna.Structure.ByValue {}
}
