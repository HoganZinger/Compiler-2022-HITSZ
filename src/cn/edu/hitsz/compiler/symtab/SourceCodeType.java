package cn.edu.hitsz.compiler.symtab;

/**
 * 源语言中的变量的类型
 *
 * @author hogan
 */
public enum SourceCodeType {
    // 我们的源语言中只有 int 变量, 对应到 RISC-V 中的 32 位有符号整数类型.
    Int,
    //增加none类型变量作为空产生式右部
    None
}
