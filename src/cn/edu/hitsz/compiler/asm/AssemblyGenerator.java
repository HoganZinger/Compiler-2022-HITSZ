package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.LinkedList;
import java.util.List;

import static cn.edu.hitsz.compiler.ir.InstructionKind.*;
import static java.lang.Integer.parseInt;


/**
 * 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    /*

    只使用 Caller 保存的 t0 到 t6,作为汇编代码中任意使用的寄存器,
    使用 a0作为程序的返回值

    *   指令	含义
        add rd, rs1, rs2	寄存器加法
        sub rd, rs1, rs2	寄存器减法
        mul rd, rs1, rs2	寄存器乘法
        addi rd, rs1, imm	立即数加法
        subi rd, rs1, imm	立即数减法
        lw rd, offset(rs1)	内存读
        sw rs2, offset(rs1)	内存写

        * 伪指令	含义
        mv rd, rs	addi rd, rs, 0
        neg rd, rs	sub x0, rs
    *
    *
    * */
    LinkedList<String> asLines=new LinkedList<>();
    LinkedList<Instruction> instructions=new LinkedList<>();

    //    enum Regs{t0,t1,t2,t3,t4,t5,t6}
    enum Regs{t4,t5,t6,t0,t1,t2,t3}
    class RegisterAllocation{
        private LinkedList<IRVariable> irvars;
        private LinkedList<Regs> regs;
        private RegisterAllocation(){
            irvars=new LinkedList<>();
            regs=new LinkedList<>();
            for(Regs r:Regs.values()){
                irvars.add(null);   regs.add(r);
            }
        }
        /**
         * 给寄存器newReg以新的ir变量newIrv
         * */
        public void reAllocate(Regs newReg,IRVariable newIrv){
            for(int i=0;i<7;i++){
                if(regs.get(i).equals(newReg)){
                    irvars.set(i,newIrv);
                }
            }
        }
        /**
         * 找到一个空的寄存器。若没有空的则返回null或报错
         * */
        public Regs findFreeReg(int currLine){
            CheckFreeArgs(currLine);
            for(int i=0;i<7;i++){
                if(irvars.get(i)==null){
                    return regs.get(i);
                }
            }
            return null;
        }

        /**
         * 释放“截止到当前行数，已经没用了”的寄存器
         * 方法：下次regToCheck在result位置出现之前，
         * 是否在from/lhs/rhs出现过？
         * 是->还有用
         * 否->没用了
         * */
        private void CheckFreeArgs(int currLine) {
            for(int i=0;i<7;i++){
                Regs regToCheck=regs.get(i);
                boolean free=true,needExplore=true;

                for(int j=currLine;j<instructions.size();j++){
                    if(!free || !needExplore){break;}
                    Instruction instr=instructions.get(j);
                    if(instr.getKind()==RET){
                        //既然都读到return了那就啥也不用干

                    }else if(instr.getKind()==MOV){
                        if(ifInReg(instr.getResult())){
                            if(regToCheck.equals(findGiven((IRVariable) instr.getResult()))){
                                //没用
                                needExplore=false;
                            }
                        }
                        if(instr.getFrom().isImmediate())continue;
                        if(ifInReg((IRVariable) instr.getFrom())){
                            if(regToCheck.equals(findGiven((IRVariable) instr.getFrom()))){
                                //有用
                                free=false;
                            }
                        }
                    }else{
                        if(ifInReg(instr.getResult())){
                            if(regToCheck.equals(findGiven((IRVariable) instr.getResult()))){
                                //没用
                                needExplore=false;
                            }
                        }
                        if(instr.getLHS().isImmediate())continue;
                        if(ifInReg((IRVariable) instr.getLHS())){
                            if(regToCheck.equals(findGiven((IRVariable) instr.getLHS()))){
                                //有用
                                free=false;
                            }
                        }
                        if(instr.getRHS().isImmediate())continue;
                        if(ifInReg((IRVariable) instr.getRHS())){
                            if(regToCheck.equals(findGiven((IRVariable) instr.getRHS()))){
                                //有用
                                free=false;
                            }
                        }


                    }
                }
                if(free){
                    irvars.set(i,null);
                }
            }
        }

        /**
         * ir变量irv是否在寄存器中
         * */
        public boolean ifInReg(IRVariable irv){
            for(int i=0;i<7;i++){
                if(irv.equals(irvars.get(i))){
                    return true;
                }
            }
            return false;
        }
        /**
         * irv保存在哪个寄存器中
         * */
        public Regs findGiven(IRVariable irv){
            for(int i=0;i<7;i++){
                if(irv.equals(irvars.get(i))){
                    return regs.get(i);
                }
            }
            return null;
        }
    }




    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // 读入前端提供的中间代码并生成所需要的信息
        instructions= (LinkedList<Instruction>) originInstructions;
        int maxTempUsed=0;
        for(int i=0;i<instructions.size();i++){
            Instruction instr=instructions.get(i);
            if(instr.getKind()!=RET){
                IRVariable irv=instr.getResult();
                if(irv.toString().matches("\\$[0-9]+")){
                    int par=Integer.parseInt(irv.toString().substring(1));
                    maxTempUsed= Math.max(par, maxTempUsed);
                }
            }
        }
        System.out.println(maxTempUsed);
        //对于加法减法乘法，如果左操作数是立即数，要新建一个irvalue储存这个立即数
        //比如(SUB, $0, 3, a)
        //要拆成(MOV, $6, 3) (SUB, $0, $6, a)
        for(int i=0;i<instructions.size();i++){
            Instruction instr=instructions.get(i);
            if(instr.getKind()==ADD ||
                    instr.getKind()==SUB ||
                    instr.getKind()==MUL
            ){
                IRValue left=instr.getLHS();
                if(left.isImmediate()){
                    IRVariable temp= IRVariable.temp();
                    IRVariable result= instr.getResult();
                    IRValue rhs= instr.getRHS();

                    Instruction ins1=Instruction.createMov(temp,left);
                    Instruction ins2=
                            instr.getKind()==ADD ? Instruction.createAdd(result,temp,rhs):
                                    instr.getKind()==SUB ? Instruction.createSub(result,temp,rhs):
                                            instr.getKind()==MUL ? Instruction.createMul(result,temp,rhs):null;

                    instructions.remove(i);
                    instructions.add(i,ins1);
                    instructions.add(i+1,ins2);

                }
            }
        }

    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // 执行寄存器分配与代码生成
        asLines.add(".text\n");
        RegisterAllocation rAlloc=new RegisterAllocation();

        int len=instructions.size();
        Instruction instr;
        for(int currLine=0;currLine<len;currLine++){
            instr=instructions.get(currLine);
            switch (instr.getKind()){
                case ADD -> {
                    IRVariable result= instr.getResult();
                    IRValue left=instr.getLHS(),
                            right=instr.getRHS();
                    //找寄存器。如果没有对应该irvalue的寄存器，就给一个新的。
                    Regs regResult,
                            regL=rAlloc.findGiven((IRVariable) left);
                    if(rAlloc.ifInReg(result)){
                        regResult=rAlloc.findGiven(result);
                    }
                    else{
                        regResult=rAlloc.findFreeReg(currLine);
                        rAlloc.reAllocate(regResult,result);
                    }
                    if(right.isImmediate()){
                        asLines.add("addi "+regResult+","+regL+","+((IRImmediate)right).getValue()+"\n");
                    }else{
                        Regs regR=rAlloc.findGiven((IRVariable) right);
                        asLines.add("add "+regResult+","+regL+","+regR+"\n");
                    }

                }
                case MUL -> {
                    IRVariable result= instr.getResult();
                    IRValue left=instr.getLHS(),
                            right=instr.getRHS();
                    //找寄存器。如果没有对应该irvalue的寄存器，就给一个新的。
                    Regs regResult,
                            regL=rAlloc.findGiven((IRVariable) left);
                    if(rAlloc.ifInReg(result)){
                        regResult=rAlloc.findGiven(result);
                    }
                    else{
                        regResult=rAlloc.findFreeReg(currLine);
                        rAlloc.reAllocate(regResult,result);
                    }
                    if(right.isImmediate()){
                        asLines.add("mul "+regResult+","+regL+","+((IRImmediate)right).getValue()+"\n");
                    }else{
                        Regs regR=rAlloc.findGiven((IRVariable) right);
                        asLines.add("mul "+regResult+","+regL+","+regR+"\n");
                    }
                }
                case SUB -> {
                    IRVariable result= instr.getResult();
                    IRValue left=instr.getLHS(),
                            right=instr.getRHS();
                    //找寄存器。如果没有对应该irvalue的寄存器，就给一个新的。
                    Regs regResult,
                            regL=rAlloc.findGiven((IRVariable) left);
                    if(rAlloc.ifInReg(result)){
                        regResult=rAlloc.findGiven(result);
                    }
                    else{
                        regResult=rAlloc.findFreeReg(currLine);
                        rAlloc.reAllocate(regResult,result);
                    }
                    if(right.isImmediate()){
                        asLines.add("sub "+regResult+","+regL+","+((IRImmediate)right).getValue()+"\n");
                    }else{
                        Regs regR=rAlloc.findGiven((IRVariable) right);
                        asLines.add("sub "+regResult+","+regL+","+regR+"\n");
                    }




                }
                case MOV -> {
                    IRValue from= instr.getFrom();
                    IRVariable result= instr.getResult();
                    System.out.println(instr);
                    //找寄存器。如果没有对应该irvalue的寄存器，就给一个新的。
                    Regs regResult;
                    if(rAlloc.ifInReg(result)){
                        regResult=rAlloc.findGiven(result);
                    }
                    else{
                        regResult=rAlloc.findFreeReg(currLine);
                        rAlloc.reAllocate(regResult,result);
                    }

                    if(from instanceof IRImmediate){
                        int imm=((IRImmediate) from).getValue();
                        //如果from是立即数
                        asLines.add("li "+regResult+","+String.valueOf(imm)+"\n");
                    }else{
                        //如果from是ir变量
                        Regs regFrom=rAlloc.findGiven((IRVariable) from);
                        asLines.add("mv "+regResult+","+regFrom+"\n");
                    }
                    rAlloc.reAllocate(regResult,result);

                }
                case RET -> {
                    IRValue irv=instr.getReturnValue();
                    Regs r=rAlloc.findGiven((IRVariable) irv);
                    asLines.add("mv a0,"+r+"\n");

                }
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // 输出汇编代码到文件
        System.out.println(asLines);
        FileUtils.writeLines(path,asLines);
    }
}

