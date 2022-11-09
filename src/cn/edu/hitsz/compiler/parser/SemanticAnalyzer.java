package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.errors.ErrorDefination;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

//实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable = null;
    private Stack<SourceCodeType> semanticTypeStack = new Stack<>();
    private Stack<Token> shiftStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        //  该过程在遇到 Accept
        semanticTypeStack.clear();
        shiftStack.clear();
        //throw new NotImplementedException();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // 该过程在遇到 reduce production 时要采取的代码动作
        switch(production.index()){

            case 4 ->{
                //S -> D id
                var id = shiftStack.pop();
                if(symbolTable.has(id.getText())){
                    var p = symbolTable.get(id.getText());
                    p.setType(semanticTypeStack.pop());
                }
                else{
                    throw new RuntimeException(String.format(ErrorDefination.NO_SYMBOL, id.getText()));
                }
            }
            case 5 -> {
                //D -> int
                semanticTypeStack.add(SourceCodeType.Int);
            }

            default -> {
                semanticTypeStack.add(SourceCodeType.None);
            }
        }

    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        //该过程在遇到 shift 时要采取的代码动作
        shiftStack.add(currentToken);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        //设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        symbolTable = table;
    }
}

