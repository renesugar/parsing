package at.searles.demo

import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.*
import at.searles.parsing.Reducer.Companion.rep
import at.searles.parsing.printing.ConcreteSyntaxTree
import at.searles.parsing.printing.CstPrinter
import at.searles.parsing.printing.StringOutStream
import at.searles.parsing.ref.Ref
import at.searles.regexparser.RegexpParser

/**
 * Demo of inversion of a parser for the following grammar:
 *
 * num: [0-9]* ;
 * term: num | '(' sum ')' ;
 * literal: '-'? term ;
 * product: term ('*' term | '/' term)* ;
 * sum: product ('+' product | '-' product)* ;
 */
fun main() {
    // Create a lexer
    val lexer = SkipTokenizer(Lexer())
            
    // ignore white spaces
    val wsTokenId = lexer.add(RegexpParser.parse("[\n\r\t ]+"))
    lexer.addSkipped(wsTokenId)

    // num: [0-9]* ;
    val numTokenId = lexer.add(RegexpParser.parse("[0-9]+"))
    val numMapping = object : Mapping<CharSequence, AstNode> {
        override fun parse(stream: ParserStream, input: CharSequence): AstNode =
                NumNode(Integer.parseInt(input.toString()))

        override fun left(result: AstNode): CharSequence? =
                if (result is NumNode) result.value.toString() else null
    }

    val num = Parser.fromToken(numTokenId, lexer, numMapping).ref("num")

    // term: num | '(' sum ')'
    val sum = Ref<AstNode>("sum")

    val openPar = Recognizer.fromString("(", lexer)
    val closePar = Recognizer.fromString(")", lexer)

    val term = (num or openPar + sum + closePar).ref("term")

    // literal: '-'? term
    // it is actually much easier to rewrite this rule
    // literal: '-' term | term


    val minus = Recognizer.fromString("-", lexer)

    val negate = object : Mapping<AstNode, AstNode> {
        override fun parse(stream: ParserStream, input: AstNode): AstNode =
                OpNode(Op.Neg, input)

        override fun left(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Neg) result.args[0] else null
    }

    val literal =
            ((minus + term + negate) or term).ref("literal")

    // product: term ('*' term | '/' term)* ;

    val times = Recognizer.fromString("*", lexer)

    val multiply = object : Fold<AstNode, AstNode, AstNode> {
        override fun apply(stream: ParserStream, left: AstNode, right: AstNode): AstNode =
                OpNode(Op.Mul, left, right)

        override fun leftInverse(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Mul) result.args[0] else null

        override fun rightInverse(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Mul) result.args[1] else null
    }

    val slash = Recognizer.fromString("/", lexer)

    val divide = object : Fold<AstNode, AstNode, AstNode> {
        override fun apply(stream: ParserStream, left: AstNode, right: AstNode): AstNode =
                OpNode(Op.Div, left, right)

        override fun leftInverse(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Div) result.args[0] else null

        override fun rightInverse(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Div) result.args[1] else null
    }

    val product = (literal + ((times.ref(FormatOp.Infix) + literal).plus(multiply) or
            (slash.ref(FormatOp.Infix) + literal).plus(divide)).rep()).ref("product")

    // sum: product ('+' product | '-' product)* ;

    val plus = Recognizer.fromString("+", lexer)

    val add = object : Fold<AstNode, AstNode, AstNode> {
        override fun apply(stream: ParserStream, left: AstNode, right: AstNode): AstNode =
                OpNode(Op.Add, left, right)

        override fun leftInverse(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Add) result.args[0] else null

        override fun rightInverse(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Add) result.args[1] else null
    }

    val sub = object : Fold<AstNode, AstNode, AstNode> {
        override fun apply(stream: ParserStream, left: AstNode, right: AstNode): AstNode =
                OpNode(Op.Sub, left, right)

        override fun leftInverse(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Sub) result.args[0] else null

        override fun rightInverse(result: AstNode): AstNode? =
                if (result is OpNode && result.op == Op.Sub) result.args[1] else null
    }

    sum.ref = product + (
                    plus.ref(FormatOp.Infix) + product.plus(add) or
                    minus.ref(FormatOp.Infix) + product.plus(sub)
            ).rep()


    // Printing a generic Ast

    /* 1*2+3 */
    val genericAst0 = OpNode(
            Op.Add,
            OpNode(
                    Op.Mul,
                    NumNode(1),
                    NumNode(2)),
            NumNode(3))

    println("Pretty-Print: ${sum.print(genericAst0)}")

    /*  (1+2)*3 */
    val genericAst1 = OpNode(
            Op.Mul,
            OpNode(
                    Op.Add,
                    NumNode(1),
                    NumNode(2)),
            NumNode(3))


    println("Pretty-Print: ${sum.print(genericAst1)}")

    val stream = ParserStream.create(readLine()!!)
    val ast = sum.parse(stream)!!

    // now pretty-printTo the tree
    val sourceStream = StringOutStream()

    val printer = object : CstPrinter(sourceStream) {
        override fun print(tree: ConcreteSyntaxTree, label: String): CstPrinter =
                when (label) {
                    FormatOp.Infix -> append(" ").print(tree).append(" ")
                    else -> print(tree)
                }
    }

    val outTree = sum.print(ast)!!
    outTree.printTo(printer)
    println("Pretty-Print: $sourceStream")
}

enum class Op { Add, Sub, Mul, Div, Neg }

object FormatOp {
    const val Infix = "infix"
}

interface AstNode

class NumNode(val value: Int) : AstNode

class OpNode(val op: Op, vararg val args: AstNode) : AstNode

