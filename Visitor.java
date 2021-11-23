import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


class Identifier {
    String name;
    Register register;
    String type; // int or array
    boolean isConst;
    boolean isGlobal;
    int value;

    int dimension;  // 数组维数
    int size ; // 数组转化为一维数组后的长度
    List<Integer> length_of_each_dimension; // 保存数组每一维长度的数组
    List<String> num; // 数组的实际数据

    public Identifier(String name,Register register,boolean isConst,boolean isGlobal) {            // 变量
        this.name = name;
        this.register = register;
        this.isConst = isConst;
        this.isGlobal = isGlobal;
        this.type = "int";
    }

    public Identifier(String name,Register register,boolean isConst,boolean isGlobal,int value) {  // 常量
        this.name = name;
        this.register = register;
        this.isConst = isConst;
        this.isGlobal = isGlobal;
        this.value = value;
        this.type = "Int";
    }

    public Identifier (String name , Register register , boolean isConst, boolean isGlobal ,
                       int dimension , List<Integer> length_of_each_dimension, List<String> num) {                   // 数组
        this.name = name;
        this.register = register;
        this.isConst = isConst;
        this.isGlobal = isGlobal;
        this.dimension = dimension;
        this.length_of_each_dimension = length_of_each_dimension;
        int size = 1;
        for(int i = 0 ; i < this.length_of_each_dimension.size() ; i++ ) {
            size *= length_of_each_dimension.get(i);
        }
        this.size = size;
        this.num = num ;
        this.type = "Array";
    }


}
class Identifier_list {
    List<Identifier> list ;

    public Identifier_list() {
        this.list = new ArrayList<>();
    }
}
class Function {
    String name;
    String returnType; // int  void String ...
    List<String> parameter_list = new ArrayList<>();

    public Function(String name) {
        this.name = name;
    }
    public Function(String name,String returnType) {
        this.name = name;
        this.returnType = returnType;
    }
    public Function(String name,String returnType,List<String> parameter_list) {
        this.name = name;
        this.returnType = returnType;
        this.parameter_list = parameter_list;
    }
}
class Register {
    String name;
    String type;

    public Register(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Register(String name) {
        this.name = name;
    }
}
public class Visitor extends compUnitBaseVisitor<Object> {
    public String ans = "";
    int cnt = 0; // count register
    int cnt_block = 0;  // 块的数量
    public boolean isConst = false;  // 标记是否正在处理常量
    public boolean isGlobal = false;  // 标记是否正在处理全局变量
    public String cur_while_head;  // 标记当前while的开始，即条件判断
    public String cur_while_end;   // 标记当前while结束后的代码位置
    boolean is_break_or_continue = false;  // 标记访问的while循环中是否有break或continue
    List<Function> Function_list = new ArrayList<>();     // 函数表
    List<Register> Register_list = new ArrayList<>();     // 寄存器表

    List<Identifier> cur_identifier_list = null;  // 当前符号表
    List<Identifier_list> Identifier_table = new ArrayList<>(); // 符号表

    List<Integer> cur_array_dimension ; // 当前处理的数组每一维的长度
    int cur_array_flag ; // 遍历到正在处理的数组的哪一维

    public void init() {
        // 初始化函数表
        Function_list.add(new Function("getint","i32"));
        Function_list.add(new Function("getch","i32"));
        Function_list.add(new Function("putint","void"));
        Function_list.add(new Function("putch","void"));

        // 初始化全局块
        Identifier_list cur_identifier = new Identifier_list();
        cur_identifier_list = cur_identifier.list;
        Identifier_table.add(cur_identifier);

        ans += "declare i32 @getint()\n";
        ans += "declare i32 @getch()\n";
        ans += "declare void @putint(i32)\n";
        ans += "declare void @putch(i32)\n";
        ans += "declare void @memset(i32*  ,i32 ,i32 )\n";
    }
    // 分配寄存器
    public Register Allocate(String type) {
        String name = "%"+(++cnt);
        Register reg = new Register(name,type);
        Register_list.add(reg);
        return reg;
    }
    public Register Allocate() {
        String name = "%"+(++cnt);
        Register reg = new Register(name);
        Register_list.add(reg);
        return reg;
    }

    // 分配 块
    public String newBlock() {
        return "block_" + (cnt_block++);
    }

    // 运算符转换成对应语法
    public String getOp(String s) {
        if( s.equals("+") ) return "add i32 ";
        if( s.equals("-") ) return "sub i32 ";
        if( s.equals("*") ) return "mul i32 ";
        if( s.equals("/") ) return "sdiv i32 ";
        if( s.equals("%") ) return "srem i32 ";
        if( s.equals("==") ) return "eq ";
        if( s.equals("!=") ) return "ne ";
        if( s.equals("<") ) return "slt ";
        if( s.equals("<=") ) return "sle ";
        if( s.equals(">") ) return "sgt ";
        if( s.equals(">=") ) return "sge ";
        if( s.equals("!") ) return "icmp eq i32 ";
        return null;
    }

    public int Calculate(int a, int b, String Op) {
        if(Op.equals("+"))  return a + b;
        if(Op.equals("-"))  return a - b;
        if(Op.equals("*"))  return a * b;
        if(Op.equals("/"))  return a / b;
        if(Op.equals("%"))  return a % b;
        if(Op.equals("==")) {
            if(a==b) return 1;
            else return 0;
        }
        if(Op.equals("<")) {
            if(a < b) return 1;
            else return 0;
        }
        if(Op.equals("<=")) {
            if(a <= b) return 1;
            else return 0;
        }
        if(Op.equals(" > ")) {
            if(a > b) return 1;
            else return 0;
        }
        if(Op.equals(" >= ")) {
            if(a >= b) return 1;
            else return 0;
        }
        return 0;
    }

    int getValue_byName(String name) {
        int size = Identifier_table.size();
        for(int i = size-1 ; i>=0 ; i--) {
            Identifier_list tmp = Identifier_table.get(i);
            List<Identifier> list = tmp.list;
            for (Identifier identifier : list) {
                if (identifier.name.equals(name)) {
                    return identifier.value;
                }
            }
        }
        return -1;
    }

    Identifier getArray_byName(String name) {
        int size = Identifier_table.size();
        for(int i=size-1; i>=0; i--) {
            Identifier_list tmp = Identifier_table.get(i);
            List<Identifier> list = tmp.list;
            for( Identifier identifier : list) {
                if(identifier.name.equals(name) && identifier.type.equals("Array")) {
                    return identifier;
                }
            }
        }
        return null;
    }

    // 数制转换
    public String getNumber(String num) {
        int val = 0;
        if (num.charAt(0)=='0'&&num.length()>1){
            if(num.charAt(1)=='x'||num.charAt(1)=='X') {
                num = num.toLowerCase();
                for (int i = 2; i < num.length(); i++) {
                    if (num.charAt(i) >= '0' && num.charAt(i) <= '9') {
                        val = 16 * val + (int)num.charAt(i) - 48;
                    } else {
                        val = 16 * val + ((int)num.charAt(i) - 'a') + 10;
                    }
                }
            }
            else {
                for( int i = 1 ; i < num.length() ; i++ ) {
                    val = 8 * val + (int)num.charAt(i) - 48;
                }
            }
            return Integer.toString(val);
        }
        else {
            return num;
        }
    }


    // 判断变量在当前作用域是否定义
    public boolean isDefined_curField(String name) {
        for (Identifier identifier : cur_identifier_list) {
            if (identifier.name.equals(name)) return true;
        }
        return false;
    }

    // 判断变量在全部作用域是否定义
    public boolean isDefined_allField(String name) {
        int size = Identifier_table.size();
        for(int i = size-1 ; i>=0 ; i--) {
            Identifier_list tmp = Identifier_table.get(i);
            List<Identifier> list = tmp.list;
            for (Identifier identifier : list) {
//                System.out.println(identifier.name + "!!\n");  // debug
                if (identifier.name.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 判断是否是常量
    public boolean isConstant(String name) {
        int size = Identifier_table.size();
        for(int i = size-1 ; i>=0 ; i--) {
            Identifier_list tmp = Identifier_table.get(i);
//            System.out.println(i);
            List<Identifier> list = tmp.list;
//            System.out.println(list);
            for (Identifier identifier : list) {
                if (identifier.name.equals(name)) {
//                    System.out.println(name);
                    return identifier.isConst;
                }
            }
        }
        return false;
    }

    // 判断函数是否定义
    public boolean Function_is_defined(String name) {
        for (Function function : Function_list) {
            if (function.name.equals(name)) return true;
        }
        return false;
    }

    // 获取变量的寄存器号
    public String getRegister(String name) {
        int size = Identifier_table.size();
        for(int i = size-1 ; i>=0 ; i--) {
            Identifier_list tmp = Identifier_table.get(i);
            List<Identifier> list = tmp.list;
            for (Identifier identifier : list) {
                if (identifier.name.equals(name)) return identifier.register.name;
            }
        }
        return null;
    }

    @Override
    public Object visitCompUnit(compUnitParser.CompUnitContext ctx) {
        if(ctx.decl()!=null) {
            isGlobal = true;
            int n = ctx.decl().size();
            for(int i=0;i<n;i++) {
                visitDecl(ctx.decl(i));
            }
            isGlobal = false;
        }
        visitFuncDef(ctx.funcDef());
        return null;
    }

    @Override
    public Void visitFuncDef(compUnitParser.FuncDefContext ctx) {
        ans += "define dso_local i32 @main()";
        ans += "{\n";
        visitBlock(ctx.block());
        ans += "\n}\n";
        return null;
    }

    @Override
    public Void visitBlock(compUnitParser.BlockContext ctx) {   // '{' (blockItem )* '}'
        // create new Identifier_list and add it into Identifier_table
        Identifier_list cur = new Identifier_list();
        cur_identifier_list = cur.list;
        Identifier_table.add(cur);

        int n = ctx.blockItem().size();
        for(int i=0 ; i<n ; i++) {
            visitBlockItem(ctx.blockItem(i));
        }

        // remove Identifier_list from Identifier_table
        Identifier_table.remove(cur);
        if(Identifier_table.size()>0)
            cur_identifier_list = Identifier_table.get( Identifier_table.size() - 1 ).list;
        else
            cur_identifier_list = null;
        return null;
    }

    @Override
    public String visitBlockItem(compUnitParser.BlockItemContext ctx) {
        if(ctx.decl() == null) {
            visitStmt(ctx.stmt());
        }
        else {
            visitDecl(ctx.decl());
        }
        return null;
    }

    @Override
    public String visitDecl(compUnitParser.DeclContext ctx) {
        if(ctx.constDecl()==null)  visitVarDecl(ctx.varDecl());
        else visitConstDecl(ctx.constDecl());
        return null;
    }

    @Override
    public String visitVarDecl(compUnitParser.VarDeclContext ctx) {
        int n = ctx.varDef().size();
        for(int i=0 ; i<n ; i++) {
            visitVarDef(ctx.varDef(i));
        }
        return null;
    }

    @Override
    public String visitVarDef(compUnitParser.VarDefContext ctx) {  // Ident ('[' constExp ']')* '=' initVal ;
        String name = ctx.Ident().getText();
        if(isDefined_curField(name)) System.exit(-1);

        if(ctx.constExp().size()==0)  {   ///////////// int
            if(isGlobal) {
                int val = 0;
                if(ctx.initVal()!=null) {
                    Object ret = visitInitVal(ctx.initVal());
                    if(ret instanceof Integer) val = (Integer)ret;
                }
                Register reg = new Register("@"+ name , "i32");
                Identifier identifier = new Identifier(name,reg,false,isGlobal,val);
                cur_identifier_list.add(identifier);
                ans += "@" + name + " = " + "dso_local global i32 " + val + "\n";
            }
            else {
                Register reg = Allocate("i32");
                ans += reg.name + " = alloca i32\n";
                // 加入变量池
                Identifier identifier = new Identifier(name,reg,false,isGlobal);
//                System.out.println(identifier.name + "\n"); //debug
                cur_identifier_list.add(identifier);
                // 赋值
                if(ctx.initVal()!=null) {
                    Object ret = visitInitVal(ctx.initVal());
                    String L = "" ;
                    if(ret instanceof Integer) { L = ((Integer) ret).toString(); }
                    else if( ret instanceof Register ) {
                        Register R = (Register)ret;
                        if(R.type.equals("i32")) { L = ((Register) ret).name; }
                        else if(R.type.equals("i32*")) {
                            Register Reg =  Allocate("i32");
                            ans += Reg.name + " = load i32, i32* " + R.name + "\n";
                            L = Reg.name;
                        }
                        else { System.exit(-56); }
                    }
                    else System.exit(-57);
                    ans += "store i32 " + L + " , " + "i32* " + reg.name + "\n" ;
                }
            }
        }
        else {                                                        ////////// Array
            int D = ctx.constExp().size();
            List<Integer> list = new ArrayList<>();                   // 当前数组每一维的长度
            // 分配寄存器
            Register reg ;
            if(isGlobal) reg = new Register("@" + name,"i32*");
            else reg = Allocate("i32*");

            for(int i=0; i<D ;i++) {
                Object d = visitConstExp(ctx.constExp(i));
                if(d instanceof Integer) {
                    if( ((Integer)d) <= 0 ) System.exit(-87) ;  // 每一维的长度需要大于零
                    list.add((Integer) d);
                }
            }
            if(ctx.initVal() == null)  {   // 没有显式赋初值
//                ans += "hhhh\n";
                Identifier I = new Identifier(name , reg , isConst , isGlobal , D , list , null);
                cur_identifier_list.add(I);
                List<String> num = new ArrayList<>();
                for(int i=0 ; i<I.size ; i++) num.add("0");
                I.num = num;  // 默认赋值为0
                if(isGlobal) {   // 全局数组，不需要输出具体赋值语句
                    ans += reg.name + " = dso_local global " + "[" + I.size + " x i32] " + "zeroinitializer\n";
                }
                else {
                    ans += reg.name + " = alloca " + "[" + I.size + " x i32]\n";
                    for(int i = 0 ; i < I.size ; i++) {
                        Register R = Allocate("i32");
//                        ans += "store i32 " + i + ", " + "i32* " + R.name + "\n";
                        ans += R.name + " = getelementptr" + "[" + I.size + " x i32] , [" + I.size + " x i32]* "
                                + reg.name + ", i32 0 , i32 " + i + "\n";
                        ans += "store i32 " + I.num.get(i) + " , i32* " + R.name + "\n";
                    }
                }
            }
            else {   // 进行了显式赋值

                cur_array_dimension = list ;
                cur_array_flag = 0;
                Object ret = visitInitVal(ctx.initVal());

                if(ret instanceof List) {
                    Identifier I = new Identifier(name , reg , isConst , isGlobal , D , list , (List<String>) ret);
                    cur_identifier_list.add(I);
                    if(isGlobal) {
                        ans += reg.name + " = dso_local global " + "[" + I.size + " x i32] [";
                        for (int i = 0 ; i < I.size ; i++) {
                            ans += "i32 " + I.num.get(i) ;
                            if(i < I.size-1) ans += ", ";
                        }
                        ans += "]\n";
                    }
                    else {
                        ans += reg.name + " = alloca " + "[" + I.size + " x i32]\n";
                        for(int i = 0 ; i < I.size ; i++) {
                            Register R = Allocate("i32");
                            ans += R.name + " = getelementptr" + "[" + I.size + " x i32] , [" + I.size + " x i32]* "
                                    + reg.name + ", i32 0 , i32 " + i + "\n";
                            ans += "store i32 " + I.num.get(i) + " , i32* " + R.name + "\n";
                        }
                    }
                }

            }
        }
        return null;
    }

    @Override
    public Object visitInitVal(compUnitParser.InitValContext ctx) {  // initVal : exp | '{' ( initVal (',' initVal )* )? '}'
        if(ctx.exp() != null) {  return visitExp(ctx.exp());  }    // exp
        else {                                                     // '{' ( initVal (',' initVal )* )? '}'
            int M = cur_array_dimension.get(cur_array_flag);
            int N = ctx.initVal().size();
            List<String> ret = new ArrayList<>();
            cur_array_flag ++;      // ********************
            for(int i=0 ; i<N ;i++) {
                Object o = visitInitVal(ctx.initVal(i));
                if(o instanceof Integer) ret.add( ((Integer) o).toString() );
                else if ( o instanceof Register ) ret.add( ((Register) o).name );
                else if( o instanceof List ) {
                    List<String> O = ((List<String>) o);
                    ret.addAll(O);
                }
                else System.exit(-92);
            }
            cur_array_flag --;      // ********************
            if(N < M) {
                int len = 1;
                for(int j = cur_array_flag+1 ; j < cur_array_dimension.size() ; j++){
                    len *= cur_array_dimension.get(j);
                }
                ArrayList<String> zero = new ArrayList<>();
                for(int j=0 ; j<len ; j++) zero.add("0");
                for(int i=N ; i<M ;i++ ) {
                    ret.addAll(zero);  // 补0
                }
            }
            return ret ;
        }
    }

    @Override
    public String visitConstDecl(compUnitParser.ConstDeclContext ctx) {   // constDecl  : 'const' Type constDef (',' constDef )* ';' ;
        this.isConst = true;
        int n = ctx.constDef().size();
        for(int i=0 ; i<n ; i++) {
            visitConstDef(ctx.constDef(i));
        }
        this.isConst = false;
        return null;
    }

    @Override
    public String visitConstDef(compUnitParser.ConstDefContext ctx) {  // constDef : Ident ('[' constExp']')* '=' constInitVal ;
//        String name = ctx.Ident().getText();
//        Object ret = visitConstInitVal(ctx.constInitVal());
        if(ctx.constExp().size() == 0) {                                // int
            String name = ctx.Ident().getText();
            Object ret = visitConstInitVal(ctx.constInitVal());
            int val = 0;
            if(isDefined_curField(name)) System.exit(-2);
            if(ret instanceof Integer) {
                val = (Integer) ret;
            }
            // 加入常量池
            Identifier identifier = new Identifier(name,null,true,isGlobal,val);
            cur_identifier_list.add(identifier);
        }
        else {                                                   // array
            String name = ctx.Ident().getText();
            int D = ctx.constExp().size();
            List<Integer> list = new ArrayList<>();                   // 当前数组每一维的长度
            Register reg = new Register("@" + name,"i32*");
            for(int i=0; i<D ;i++) {
                Object d = visitConstExp(ctx.constExp(i));
                if(d instanceof Integer) {
                    if( ((Integer)d) <= 0 ) System.exit(-88) ;  // 每一维的长度需要大于零
                    list.add((Integer) d);
                }
            }
            cur_array_dimension = list ;
            cur_array_flag = 0;
            Object ret = visitConstInitVal(ctx.constInitVal());

            if(ret instanceof List) {
                Identifier I = new Identifier(name , reg , isConst , isGlobal , D , list , (List<String>) ret);
                cur_identifier_list.add(I);
                String S;  // constant or global
                // 测试数据中没有局部的常量数组，但是有时间最好还是考虑一下。
                if(isConst)  S = "constant";
                else  S = "global" ;
                if( ( (List<Integer>) ret ).size() != 0 ) {
                    ans += I.register.name + " = dso_local " + S + " [" + I.size + " x i32] " + "[" ;
                    for(int i=0;i<I.size;i++) {
                        ans += "i32 " + I.num.get(i)  ;
                        if(i < I.size-1) ans += ", ";
                    }
                    ans += "]\n";
                }
                else {
                    ans += I.register.name + " = dso_local " + S + " [" + I.size + " x i32] " + "zeroinitializer\n" ;
                }
            }
        }
        return null;
    }

    @Override
    public Object visitConstInitVal(compUnitParser.ConstInitValContext ctx) {
        if(ctx.constInitVal().size()==0 && ctx.constExp()!=null)   {                   // constExp
            return visitConstExp(ctx.constExp());
        }
        else {                                                         // '{' (constInitVal ( ',' constInitVal )* )? '}'
            int M = cur_array_dimension.get(cur_array_flag);
            int N = ctx.constInitVal().size();
            List<String> ret = new ArrayList<>();
            cur_array_flag ++;  //  *********
            for(int i=0 ; i<N ;i++) {
                Object o = visitConstInitVal(ctx.constInitVal(i));
                if(o instanceof Integer) ret.add( ((Integer) o).toString() );
                else if( o instanceof List ) {
                    List<String> O = ((List<String>) o);
                    ret.addAll(O);
                }
                else System.exit(-91);
            }
            cur_array_flag --;  // *********
            if(N < M) {
                int len = 1;
                for(int j = cur_array_flag+1 ; j < cur_array_dimension.size() ; j++){
                    len *= cur_array_dimension.get(j);
                }
                ArrayList<String> zero = new ArrayList<>();
                for(int j=0 ; j<len ; j++) zero.add("0");
                for(int i=N ; i<M ;i++ ) {
                    ret.addAll(zero);  // 补0
                }
            }
            return ret ;
        }
    }

    @Override
    public Object visitConstExp(compUnitParser.ConstExpContext ctx) {
        return visitAddExp(ctx.addExp());
    }

    @Override
    public Object visitStmt(compUnitParser.StmtContext ctx) {
        if(ctx.lVal() != null) {             // LVal '=' Exp ';'

            String name = ctx.lVal().Ident().getText();
            if(!isDefined_allField(name)) System.exit(-3); // 如果变量未定义，报错
            if(isConstant(name)) System.exit(-4);
            Object lval = visitLVal(ctx.lVal());
            String S="";
            if(lval instanceof Register ) {
                if(((Register) lval).type.equals("i32*"))
                    S = ((Register)lval).name;
                else
                    S = getRegister(name);
            }
            else System.exit(-71);
            Object ret = visitExp(ctx.exp());
            String R = "";
            if(ret instanceof Integer) { R = ((Integer) ret).toString(); }
            else if( ret instanceof Register ) {
                Register Reg = (Register)ret;
                if(Reg.type.equals("i32")) { R = ((Register) ret).name; }
                else if(Reg.type.equals("i32*")) {
                    Register Reg1 =  Allocate("i32");
                    ans += Reg1.name + " = load i32, i32* " + Reg.name + "\n";
                    R = Reg1.name;
                }
                else { System.exit(-58); }
            }
            else System.exit(-59);
            ans += "store i32 " + R + " , " + "i32* " + S + "\n";
        }
        else if(ctx.Return() != null) {      // 'return' Exp ';'
            Object reg = visitExp(ctx.exp());
            String R;
            if(reg instanceof Integer) { R = ((Integer) reg).toString(); }
            else {
                R = ((Register) reg).name;
            }
            ans += "ret i32 " + R + "\n";
            Allocate("i32");  // return 后要分配一个寄存器占位
            return null;
        }
        else if(ctx.If() != null) {    // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            int stmt_len = ctx.stmt().size();
            Object reg_cond = visitCondition(ctx.condition());
            String COND;
            if(reg_cond instanceof Integer) {
                Register cond = Allocate("i1");
                ans += cond.name + " = icmp ne i32 " +  ((Integer)reg_cond).toString() + " , " +  "0" + "\n";
                COND = cond.name;
            }
            else {
                Register reg_cond1 = (Register) reg_cond;
                if (reg_cond1.type.equals("i32")) {
                    Register cur_reg = Allocate("i1");
                    ans += cur_reg.name + " = trunc i32 " + reg_cond1.name + " to i1\n";
                    COND = cur_reg.name;
                }
                else if(reg_cond1.type.equals("i32*")) {
                    Register R = Allocate("i32");
                    Register L = Allocate("i1");
                    ans += R.name + " = load i32, i32* " + reg_cond1.name + "\n" ;
                    ans += L.name + " = trunc i32 " + R.name + " to i1\n";
                    COND = L.name;
                }
                else COND = reg_cond1.name;
            }
            if(stmt_len == 1) {  // 只有if没有else
                String block_stmt = newBlock();
                String block_next = newBlock();
                ans += "br i1 " + COND + " , label %" + block_stmt + " , label %" + block_next + "\n";
                ans += "\n" + block_stmt + ":\n";
                is_break_or_continue = false;

                visitStmt(ctx.stmt().get(0));

                if(!is_break_or_continue) {
                    ans += "br label %" + block_next + "\n";
                }
                is_break_or_continue = false;
                ans += "\n" + block_next + ":\n";
            }
            else {   // if ... else ...
                String block_stmt = newBlock();
                String block_else = newBlock();
                String block_next = newBlock();
                ans += "br i1 " + COND + " , label %" + block_stmt + " , label %" + block_else + "\n";
                ans += "\n" + block_stmt + ":\n";
                is_break_or_continue = false;
                visitStmt(ctx.stmt().get(0));
                if(!is_break_or_continue)
                    ans += "br label %" + block_next + "\n";
                is_break_or_continue = false;
                ans += "\n" + block_else + ":\n";
                visitStmt(ctx.stmt().get(1));
                if(!is_break_or_continue)
                    ans += "br label %" + block_next + "\n";
                is_break_or_continue = false;
                ans += "\n" + block_next + ":\n";
            }
        }
        else if(ctx.While()!=null) {  // While '(' condition ')' stmt
            String while_head = newBlock();
            ans += "br label %" + while_head + "\n";
            ans += "\n" + while_head + ":\n";
            cur_while_head = while_head;
            Object reg_cond = visitCondition(ctx.condition());
            String COND;
            if(reg_cond instanceof Integer) {
                Register cond = Allocate("i1");
                ans += cond.name + " = icmp ne i32 " +  ((Integer)reg_cond).toString() + " , " +  "0" + "\n";
                COND = cond.name;
            }
            else {
                Register reg_cond1 = (Register) reg_cond;
                if (reg_cond1.type.equals("i32")) {
                    Register cur_reg = Allocate("i1");
                    ans += cur_reg.name + " = trunc i32 " + reg_cond1.name + " to i1\n";
                    COND = cur_reg.name;
                }
                else if(reg_cond1.type.equals("i32*")) {
                    Register R = Allocate("i32");
                    Register L = Allocate("i1");
                    ans += R.name + " = load i32, i32* " + reg_cond1.name + "\n" ;
                    ans += L.name + " = trunc i32 " + R.name + " to i1\n";
                    COND = L.name;
                }
                else COND = reg_cond1.name;
            }
            String while_begin = newBlock();
            String while_end = newBlock();
            cur_while_end = while_end;
            ans += "br i1 " + COND + " , label %" + while_begin + " , label %" + while_end + "\n";
            ans += "\n" + while_begin + ":\n";
            visitStmt(ctx.stmt().get(0));
            ans += "br label %" + while_head + "\n";
            ans += "\n" + while_end + ":\n";
        }
        else if(ctx.Continue() != null) {      // Continue
            is_break_or_continue = true;
            ans += "br label %" + cur_while_head + "\n";
            return 1;
        }
        else if(ctx.Break() != null) {        // break
            is_break_or_continue = true;
            ans += "br label %" + cur_while_end + "\n";
            return 1;
        }
        else if(ctx.block() != null) {         // Block
            visitBlock(ctx.block());
        }
        else if(ctx.exp()!=null) {     // exp
            visitExp(ctx.exp());
        }
        return null;
    }

    @Override
    public Object visitCondition(compUnitParser.ConditionContext ctx) {
        return visitLorExp(ctx.lorExp());
    }

    @Override
    public Object visitLorExp(compUnitParser.LorExpContext ctx) {
        if(ctx.lorExp() != null) {    // LOrExp '||' LAndExp
            Object l = visitLorExp(ctx.lorExp());
            Object r = visitLandExp(ctx.landExp());
            String L,R;
            if( l instanceof Integer) {
                Register reg = Allocate("i1");
                ans += reg.name + " = " + "icmp " + "ne" + " i32 " + ((Integer) l).toString() + " , " +  "0" + "\n";
                L = reg.name;
            }
            else {
                L = ((Register) l).name;
            }
            if(r instanceof Integer) {
                Register reg = Allocate("i1");
                ans += reg.name + " = " + "icmp " + "ne" + " i32 " + ((Integer) r).toString() + " , " +  "0" + "\n";
                R = reg.name;
            }
            else {
                R = ((Register) r).name;
            }
            Register cur_reg = Allocate("i1");
            ans += cur_reg.name + " = " + "or " + "i1 " + L + " , " + R + "\n";
            return cur_reg;
        }
        else {    // landExp
            return visitLandExp(ctx.landExp());
        }
    }

    @Override
    public Object visitLandExp(compUnitParser.LandExpContext ctx) {
        if(ctx.landExp() != null) {      // LAndExp '&&' EqExp
            Object l = visitLandExp(ctx.landExp());
            Object r = visitEqExp(ctx.eqExp());
            String L , R;
            if(l instanceof Integer) {
                Register reg = Allocate("i1");
                ans += reg.name + " = " + "icmp " + "ne" + " i32 " + ((Integer) l).toString() + " , " +  "0" + "\n";
                L = reg.name;
            }
            else {
                L = ((Register) l).name;
            }
            if(r instanceof Integer) {
                Register reg = Allocate("i1");
                ans += reg.name + " = " + "icmp " + "ne" + " i32 " + ((Integer) r).toString() + " , " +  "0" + "\n";
                R = reg.name;
            }
            else {
                R = ((Register) r).name;
            }
            Register cur_reg = Allocate("i1");
            ans += cur_reg.name + " = " + "and " + "i1 " + L + " , " + R + "\n";
            return cur_reg;
        }
        else {   // eqExp
            return visitEqExp(ctx.eqExp());
        }
    }

    @Override
    public Object visitEqExp(compUnitParser.EqExpContext ctx) {
        if(ctx.eqExp() != null) {    // EqExp ('==' | '!=') RelExp
            Object l = visitEqExp(ctx.eqExp());
            Object r = visitRelExp(ctx.relExp());
            String L,R;
            if(l instanceof Integer)  { L = ((Integer) l).toString(); }
            else {
                Register l1 = (Register) l;
                if(l1.type.equals("i1")) {
                    Register temp1 = Allocate("i32");
                    ans += temp1.name + " = " + "zext i1 " + l1.name + " to i32\n";
                    l1 = temp1;
                }
                else if(l1.type.equals("i32*")) {
                    Register Reg = Allocate("i32");
                    ans += Reg.name + " = load i32, i32* " + l1.name + "\n";
                    l1 = Reg;
                }
                L = l1.name;
            }
            if(r instanceof Integer) {  R = ((Integer) r).toString();  }
            else {
                Register r1 = (Register) r;
                if(r1.type.equals("i1")) {
                    Register temp2 = Allocate("i32");
                    ans += temp2.name + " = " + "zext i1 " + r1.name + " to i32\n";
                    r1 = temp2;
                }
                else if(r1.type.equals("i32*")) {
                    Register Reg = Allocate("i32");
                    ans += Reg.name + " = load i32, i32* " + r1.name + "\n";
                    r1 = Reg;
                }
                R = r1.name;
            }
            Register cur_reg = Allocate("i1");
            ans += cur_reg.name + " = " + "icmp " + getOp(ctx.Equal().getText()) + " i32 " + L + " , " + R + "\n";
            return cur_reg;
        }
        else {
            return visitRelExp(ctx.relExp());
        }
    }

    @Override
    public Object visitRelExp(compUnitParser.RelExpContext ctx) {
        if(ctx.relExp() != null) {   // RelExp ('<' | '>' | '<=' | '>=') AddExp
            Object l = visitRelExp(ctx.relExp());
            Object r = visitAddExp(ctx.addExp());
//            System.out.println("debug: " + r);
            String L,R;
//            System.out.println("debug: " + l+ "  || " + r);
            if( l instanceof Integer ) L = ((Integer) l).toString();
            else {
                Register l1 = (Register) l ;
                if(l1.type.equals("i1")) {
                    Register temp1 = Allocate("i32");
                    ans += temp1.name + " = " + "zext i1 " + l1.name + " to i32\n";
                    l1 = temp1;
                }
                else if(l1.type.equals("i32*")) {
                    Register Reg = Allocate("i32");
                    ans += Reg.name + " = load i32, i32* " + l1.name + "\n";
                    l1 = Reg;
                }
                L = l1.name;
            }
            if( r instanceof Integer ) R = ((Integer) r).toString();
            else {
                Register r1 = (Register) r ;
                if(r1.type.equals("i1")) {
                    Register temp2 = Allocate("i32");
                    ans += temp2.name + " = " + "zext i1 " + r1.name + " to i32\n";
                    r1 = temp2;
                }
                else if(r1.type.equals("i32*")) {
                    Register Reg = Allocate("i32");
                    ans += Reg.name + " = load i32, i32* " + r1.name + "\n";
                    r1 = Reg;
                }
                R = r1.name;
            }
            Register cur_reg = Allocate("i1");
            String cmp = getOp(ctx.Cmp().getText());
            ans += cur_reg.name + " = " + "icmp " + cmp + " i32 " + L + " , " +  R + "\n";
            return cur_reg;
        }
        else {
            return visitAddExp(ctx.addExp());
        }
    }

    @Override
    public Object visitExp(compUnitParser.ExpContext ctx) {
        return visitAddExp(ctx.addExp());
    }

    @Override
    public Object visitAddExp(compUnitParser.AddExpContext ctx) {
//        System.out.println("debug: " + ctx.getText());
        if(ctx.addExp()==null) {
            return visitMulExp(ctx.mulExp());
        }
        Object l = visitAddExp(ctx.addExp());
        Object r = visitMulExp(ctx.mulExp());
        if(l instanceof Integer && r instanceof Integer) {
            Integer l1 = (Integer) l;
            Integer r1 = (Integer) r;
            String Op = ctx.unaryOp().getText();
            int ret = Calculate( l1 , r1 , Op);
            return ret;
        }
        else {
            String L,R;
            if( l instanceof Integer ) {
                L = ((Integer) l).toString();
            }
            else {
                if( ((Register)l).type.equals("i32*") ) {
                    Register Reg = Allocate("i32");
                    ans += Reg.name + " = load i32, i32* " + ((Register)l).name + "\n";
                    L = Reg.name;
                }
                else {
                    L = ((Register) l).name;
                }
            }
            if( r instanceof Integer ) {
                R = ((Integer) r).toString();
            }
            else {
                if( ((Register)r).type.equals("i32*") ) {
                    Register Reg = Allocate("i32");
                    ans += Reg.name + " = load i32, i32* " + ((Register)r).name + "\n";
                    R = Reg.name;
                }
                else {
                    R = ((Register) r).name;
                }
            }
            Register ret = Allocate("i32");
            ans += ret.name + " = " + getOp(ctx.unaryOp().getText()) + L + " , " + R + "\n";
            return ret;
        }
    }

    @Override
    public Object visitMulExp(compUnitParser.MulExpContext ctx) {
//        System.out.println("debug: " + ctx.getText()); -1
        if( ctx.mulExp()==null ) {
            return visitUnaryExp(ctx.unaryExp());
        }
        Object l = visitMulExp(ctx.mulExp());
        Object r = visitUnaryExp(ctx.unaryExp());
        if(l instanceof Integer && r instanceof Integer) {
            Integer L = (Integer) l;
            Integer R = (Integer) r;
            String Op = ctx.calOp().getText();
            int ret = Calculate( L , R , Op);
            return ret;
        }
        else {
            String L,R;
            if( l instanceof Integer ) {
                L = ((Integer) l).toString();
            }
            else {
                if( ((Register)l).type.equals("i32*") ) {
                    Register Reg = Allocate("i32");
                    ans += Reg.name + " = load i32, i32* " + ((Register)l).name + "\n";
                    L = Reg.name;
                }
                else {
                    L = ((Register) l).name;
                }
            }
            if( r instanceof Integer ) {
                R = ((Integer) r).toString();
            }
            else {
                if( ((Register)r).type.equals("i32*") ) {
                    Register Reg = Allocate("i32");
                    ans += Reg.name + " = load i32, i32* " + ((Register)r).name + "\n";
                    R = Reg.name;
                }
                else {
                    R = ((Register) r).name;
                }
            }
            Register ret = Allocate("i32");
            ans += ret.name + " = " + getOp(ctx.calOp().getText()) + L + " , " + R + "\n";
            return ret ;
        }
    }

    @Override
    public Object visitUnaryExp(compUnitParser.UnaryExpContext ctx) {
        if(isConst || isGlobal) {  // 常量
            if( ctx.unaryExp()==null ) {
                return visitPrimaryExp(ctx.primaryExp());
            }
            else {  // UnaryOp UnaryExp
                Object r = visitUnaryExp(ctx.unaryExp());
                String Op = getOp(ctx.unaryOp().getText());
                if( r instanceof Integer) {
                    Integer R = (Integer) r;
                    if(Op.equals("-")) R = -R;
                    return R;
                }
            }
        }
        else {   // 变量
            if(ctx.Ident()!=null) {
                String name = ctx.Ident().getText();
                if(!Function_is_defined(name)) System.exit(-5); // 函数未定义，报错
                switch (name) {
                    case "getint": {
                        if (ctx.funcRParams() != null) System.exit(-6);
                        Register reg = Allocate("i32");
                        ans += reg.name + " = call i32 @getint()\n";
                        return reg;
                    }
                    case "putint": {
                        if (ctx.funcRParams().exp().size() != 1) System.exit(-7);
                        Object E = visitExp(ctx.funcRParams().exp(0));
                        String R = "";
                        if(E instanceof Integer) R = ((Integer)E).toString();
                        else if(E instanceof Register){
                            Register Reg = (Register) E;
                            if (Reg.type.equals("i32")) {
                                R = ((Register) Reg).name;
                            } else if (Reg.type.equals("i32*")) {
                                Register R1 = Allocate("i32");
                                ans += R1.name + " = load i32, i32* " + Reg.name + "\n";
                                R = R1.name;
                            } else {
                                System.exit(-30);
                            }
                            ans += "call void @putint(i32 " + R + ")\n";
                        }
                        else { System.exit(-31); }
                        break;
                    }
                    case "getch": {
                        if (ctx.funcRParams() != null) System.exit(-8);
                        Register reg = Allocate("i32");
                        ans += reg.name + " = call i32 @getch()\n";
                        return reg;
                    }
                    case "putch": {
                        if (ctx.funcRParams().exp().size() != 1) System.exit(-9);
                        Object E = visitExp(ctx.funcRParams().exp(0));
                        String R = "";
                        if(E instanceof Integer) R = ((Integer)E).toString();
                        else if(E instanceof Register){
                            Register Reg = (Register) E;
                            if (Reg.type.equals("i32")) {
                                R = ((Register) Reg).name;
                            } else if (Reg.type.equals("i32*")) {
                                Register R1 = Allocate("i32");
                                ans += R1.name + " = load i32, i32* " + Reg.name + "\n";
                                R = R1.name;
                            } else {
                                System.exit(-32);
                            }
                            ans += "call void @putch(i32 " + R + ")\n";
                        }
                        else { System.exit(-33); }
                        break;
                    }
                    default:
                        break;
                }
            }
            else {
                if( ctx.unaryExp()==null ) {
//                    System.out.println("here");
                    return visitPrimaryExp(ctx.primaryExp());
                }
                else {
                    String l = "0";
                    Object r = visitUnaryExp(ctx.unaryExp());
                    if(r instanceof Register) {
                        Register R = (Register) r;
                        if(R.type.equals("i1")) {
                            Register temp = Allocate("i32");
                            ans += temp.name + " = " + "zext i1 " + R.name + " to i32\n";
                            R = temp;
                        }
                        Register reg = Allocate();
                        if(ctx.unaryOp().getText().equals("!")) {
                            reg.type = "i1";
                        }
                        else {
                            reg.type = "i32";
                        }
                        ans += reg.name + " = " + getOp(ctx.unaryOp().getText()) + l + " , " + R.name + "\n";
                        return reg;
                    }
                    else if(r instanceof Integer){
                        String Op = getOp(ctx.unaryOp().getText());
                        Integer R = (Integer) r;
                        if(Op.equals("-")) R = -R;
                        return R;
                    }
                    else { System.exit(-97); }
                }
            }
        }
        return null;
    }

    @Override
    public String visitFuncRParams(compUnitParser.FuncRParamsContext ctx) {
        int n = ctx.exp().size();
        for(int i=0 ; i<n ;i++) {
            visitExp(ctx.exp(i));
        }
        return null;
    }
    @Override
    public Object visitLVal(compUnitParser.LValContext ctx) {    //  Ident {'[' Exp ']'}
        String name = ctx.Ident().getText();
        if(!isDefined_allField(name)) {
            System.exit(-10);
        }
        if(!isConstant(name) && ( isConst || isGlobal)) {
            System.exit(-11);
        }
        if( ctx.exp().size() == 0 ) {                     // 数
            if(isConstant(name)) {  // 常量返回int
                int val = getValue_byName(name);
                return val;
            }
            else {  // 变量返回寄存器
                String reg = getRegister(name);
                Register newReg = Allocate("i32");
                ans += newReg.name + " = load i32, i32* " + reg + "\n";
                return newReg;
            }
        }
        else {                                     // 数组
            int N = ctx.exp().size();
            Identifier I = getArray_byName(name);
            if(I==null) System.exit(-56);  // 数组未定义
            if(N != I.dimension) System.exit(-57); // 索引维数不对
            String cur_Address = "0";
            for( int i = 0 ; i < N ; i++  ) {
                Object O = visitExp(ctx.exp(i));
                if( O instanceof Integer ) {
                    int x = 1;
                    for(int j = i+1 ; j<N ;j++) x *= I.length_of_each_dimension.get(j);
                    Register R = Allocate("i32");
                    //  %10 = mul i32 %9, 2
                    ans += R.name + " = mul i32 " + (Integer)O + " , " + x + "\n";
                    Register R2 = Allocate("i32");
                    ans += R2.name + " = add i32 " + cur_Address + " , " + R.name + "\n";
                    cur_Address = R2.name;
                }
                else if ( O instanceof Register) {
                    Register o = (Register) O;
                    String Base;
                    if(o.type.equals("i32*")) {
                        Register R0 = Allocate("i32");
                        ans += R0.name + " = load i32, i32* " + o.name;
                        Base = R0.name;
                    }
                    else Base = o.name;

                    int x = 1;
                    for(int j = i+1 ; j<N ;j++) x *= I.length_of_each_dimension.get(j);
                    Register R = Allocate("i32");
                    ans += R.name + " = mul i32 " + Base + " , " + x + "\n";
                    Register R2 = Allocate("i32");
                    ans += R2.name + " = add i32 " + cur_Address + " , " + R.name + "\n";
                    cur_Address = R2.name;
                }
                else  System.exit(-111);
            }
            // %3 = getelementptr [20 x i32], [20 x i32]* @a, i32 0, i32 %2 ; %3 类型为 i32*
            Register reg = Allocate("i32*");
            ans += reg.name + " = getelementptr [" + I.size + " x i32], [" + I.size + " x i32]* " + I.register.name
                    + " , i32 0, i32 " + cur_Address + "\n";
            return reg;
        }
    }

    // primaryExp   : '(' exp ')' | lVal | Number ;
    @Override
    public Object visitPrimaryExp(compUnitParser.PrimaryExpContext ctx) {
//        System.out.println("Debug: " + ctx.getText());
        if(isConst || isGlobal) { // 常量
            if(ctx.exp() == null) {
                if(ctx.lVal() == null) {  // number
//                    System.out.println("debug:  " + ctx.Number().getText());
                    int number = Integer.parseInt( getNumber(ctx.Number().getText()) );
                    return number;
                }
                else {
                    String ret = ctx.lVal().Ident().getText(); // 变量名
                    System.out.println(ret);
                    if(!isConstant(ret)) System.exit(-77); // 检查是否是常量
                    // 需要根据常量名获取值
                    int val = getValue_byName(ret);
                    return val;
                }
            }
            else {
                return visitExp(ctx.exp());
            }
        }
        else {  // 变量
            if(ctx.exp() == null) {
                if(ctx.lVal()==null) {  // Number
//                    System.out.println("Here");
                    int number = Integer.parseInt( getNumber(ctx.Number().getText()) );
//                    System.out.println("debug:" + number);
                    return number;
                }
                else {
                    return visitLVal(ctx.lVal());
                }
            }
            else
                return visitExp(ctx.exp());
        }
    }

}
