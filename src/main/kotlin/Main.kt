package net.sebyte

import com.strumenta.antlrkotlin.parsers.generated.SQLiteLexer
import com.strumenta.antlrkotlin.parsers.generated.SQLiteParser
import com.strumenta.antlrkotlin.parsers.generated.SQLiteParserBaseVisitor
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.tree.RuleNode
import org.antlr.v4.kotlinruntime.tree.SyntaxTree
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import org.antlr.v4.kotlinruntime.tree.Tree
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.measureTime

private class QueryPrinter(
    private val needMap: Map<Tree, Boolean>
) : SQLiteParserBaseVisitor<String>() {

    private fun needed(node: Tree) = needMap.getOrDefault(node, true)

    override fun defaultResult() = ""

    override fun visitChildren(node: RuleNode): String = buildString {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (!needed(child)) continue
            append(child.accept(this@QueryPrinter).trim())
            append(" ")
        }
    }

    override fun visitTerminal(node: TerminalNode) =
        if (node.symbol.type == Token.EOF) ""
        else if (needed(node)) node.text
        else ""
}

private class TokenCounter(
    private val needMap: Map<Tree, Boolean> = emptyMap(),
) : SQLiteParserBaseVisitor<Int>() {

    private fun needed(node: Tree) = needMap.getOrDefault(node, true)

    override fun defaultResult() = 0

    override fun visitChildren(node: RuleNode): Int {
        var count = 0
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (!needed(child)) continue
            count += child.accept(this@TokenCounter)
        }
        return count
    }

    override fun visitTerminal(node: TerminalNode) =
        if (node.symbol.type == Token.EOF) 0
        else if (needed(node)) 1
        else 0
}

fun main(args: Array<String>) {
    val argParser = ArgParser("reducer")
    val fileName by argParser.option(ArgType.String, "query", "q", "Query file").required()
    val testName by argParser.option(ArgType.String, "test", "t", "Test script").required()
    val verbose by argParser.option(ArgType.Boolean, "verbose", "v", "Print verbose logs").default(false)
    val stats by argParser.option(ArgType.Boolean, "stats", "s", "Print stats").default(false)
    val quick by argParser.option(
        ArgType.Boolean,
        "quick",
        description = "Skip last delta debugging pass after hierarchical delta debugging"
    ).default(false)
    argParser.parse(args)

    val file = File(fileName)
    val reducedFile = File("./query.sql")
    val stream = CharStreams.fromFileName(file.absolutePath)
    val lexer = SQLiteLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = SQLiteParser(tokens)
    val parse = parser.parse()
    if (parser.numberOfSyntaxErrors > 0) println("-- The query contains an SQL query syntax error! Might be a dot commands (.some command) which we currently cannot handle.")

    val needMap = mutableMapOf<Tree, Boolean>()
    fun needed(node: Tree) = needMap.getOrDefault(node, true)

    fun verbose(any: Any?) {
        if (verbose) println("-- $any")
    }

    fun runScript() = ProcessBuilder(testName).start().waitFor() == 0

    fun getNodesOfLevel(level: Int, node: SyntaxTree = parse): List<SyntaxTree> =
        if (!needed(node)) emptyList()
        else if (level == 0) listOf(node)
        else if (node.childCount == 0) emptyList()
        else List(node.childCount) { node.getChild(it) }
            .map { it as SyntaxTree }
            .filter { needed(it) }
            .flatMap { getNodesOfLevel(level - 1, it) }

    fun allNodes(node: SyntaxTree = parse): List<SyntaxTree> =
        if (!needed(node)) emptyList()
        else if (node.childCount == 0) listOf(node)
        else List(node.childCount) { node.getChild(it) }
            .map { it as SyntaxTree }
            .filter { needed(it) }
            .flatMap { allNodes(it) }

    fun query(): String = QueryPrinter(needMap).visit(parse)

    var tests = 0
    fun test(): Boolean {
        ++tests
        val sql = query()
        reducedFile.writeText(sql)
        return runScript()
    }

    fun ddmin(nodes: List<SyntaxTree>, n0: Int = 2): List<SyntaxTree> {
        for (n in max(2, n0)..nodes.size) {
            val deltas: List<List<SyntaxTree>> = List(n) {
                val size = nodes.size / n
                nodes.subList(it * size, (it + 1) * size)
            }

            for ((d, delta) in deltas.withIndex()) {
                for (node in nodes) needMap[node] = false
                for (node in delta) needMap[node] = true
                if (test()) return ddmin(delta)

                val complement = deltas.filterIndexed { i, _ -> i != d }.flatten()
                for (node in nodes) needMap[node] = false
                for (node in complement) needMap[node] = true
                if (test()) return ddmin(complement, n - 1)
            }
        }
        return nodes
    }

    fun prune(nodes: List<SyntaxTree>, minconfig: List<SyntaxTree>) {
        for (node in nodes) needMap[node] = false
        for (node in minconfig) needMap[node] = true
    }

    var nodes = getNodesOfLevel(0)
    val firstTime = measureTime {
        var level = 0
        while (nodes.isNotEmpty()) {
            verbose("Level: $level")
            verbose("Nodes: ${nodes.size}")
            val minconfig = ddmin(nodes)
            verbose("Minconfig: ${minconfig.size}")
            prune(nodes, minconfig)
            ++level
            nodes = getNodesOfLevel(level)
        }
    }

    val beforeFinal = TokenCounter(needMap).visit(parse)
    val beforeFinalTests = tests
    val secondTime = measureTime {
        if (!quick) {
            verbose("$tests tests before final pass")
            nodes = allNodes()
            val minconfig = ddmin(nodes)
            prune(nodes, minconfig)
            verbose("Final pass from ${nodes.size} to ${minconfig.size}")
        }
    }

    if (stats) {
        val original = TokenCounter().visit(parse)
        val reduced = TokenCounter(needMap).visit(parse)
        verbose("Original Query: $original tokens")
        verbose("Reduced Query: $reduced tokens")
        val reduction = (1 - reduced.toDouble() / original) * 100
        verbose("%.4f%% reduction".format(reduction))
        verbose("$tests tests")
        verbose("HDD: ${firstTime.inWholeSeconds}s")
        verbose("Last pass: ${secondTime.inWholeSeconds}s")
        verbose("Total time: ${(firstTime + secondTime).inWholeSeconds}s")
        println("""
            /*
            Summary:
            $original original
            $beforeFinal before final (-${100.0 - (1000.0 * beforeFinal / original).roundToInt() / 10.0}%)
            $reduced final (-${(100.0 - (1000.0 * reduced / original).roundToInt() / 10.0)}%)
            
            HDD: ${firstTime.toString(DurationUnit.SECONDS, 1)}
            Last pass: ${secondTime.toString(DurationUnit.SECONDS, 1)}
            Total: ${(firstTime + secondTime).toString(DurationUnit.SECONDS, 1)}
            
            $beforeFinalTests before final
            $tests total
            */
        """.trimIndent())
    }
    val final = query()
    println(final)
    reducedFile.writeText(final)
}