import org.junit.Assert
import org.junit.Test
import java.lang.AssertionError

class ParserTest {

    @Test
    fun testLetStatements() {
        listOf(
                Triple("let x = 5;", "x", 5),
                Triple("let y = true;", "y", true),
                Triple("let foobar = y;", "foobar", "y")
        ).forEach { (input, expectedName, expectedValue) ->
            val parser = Parser(input)
            val program = parser.parseProgram()
            parser.checkErrors()

            val l = (program.statements.firstOrNull() as? LetStatement)
            Assert.assertNotNull(l)
            Assert.assertEquals(TokenType.LET, l?.token?.type)
            l?.name?.testLiteral(expectedName)
            l?.value?.testLiteral(expectedValue)
        }
    }

    @Test
    fun testReturnStatements() {
        listOf(
                "return 5;" to 5,
                "return false;" to false,
                "return y;" to "y"
        ).forEach { (input, expectedValue) ->
            val parser = Parser(input)
            val program = parser.parseProgram()
            parser.checkErrors()

            val r = (program.statements.firstOrNull() as? ReturnStatement)
            Assert.assertNotNull(r)
            Assert.assertEquals(TokenType.RETURN, r?.token?.type)
            r?.value?.testLiteral(expectedValue)
        }
    }

    @Test
    fun testIdentifierExpression() {
        val input = "foobar;"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val stmt = program.statements.singleOrNull() as? ExpressionStatement
        Assert.assertNotNull(stmt)
        stmt?.expression?.testLiteral("foobar")
    }

    @Test
    fun testIntegerExpression() {
        val input = "5;"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val stmt = program.statements.singleOrNull() as? ExpressionStatement
        Assert.assertNotNull(stmt)
        stmt?.expression?.testLiteral(5)
    }

    @Test
    fun testBoolExpression() {
        val input = "false;"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val stmt = program.statements.singleOrNull() as? ExpressionStatement
        Assert.assertNotNull(stmt)
        stmt?.expression?.testLiteral(false)
    }

    @Test
    fun testStringExpression() {
        val input = "\"hello world\";"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val stmt = program.statements.singleOrNull() as? ExpressionStatement
        Assert.assertNotNull(stmt)
        stmt?.expression?.testLiteral("hello world")
    }

    @Test
    fun testPrefixExpressions() {
        listOf(
                Triple("!5", "!", 5),
                Triple("-15", "-", 15),
                Triple("!true;", "!", true),
                Triple("!false", "!", false)
        ).forEach { (input, operator, right) ->
            val parser = Parser(input)
            val program = parser.parseProgram()
            parser.checkErrors()

            val stmt = program.statements.singleOrNull() as? ExpressionStatement
            Assert.assertNotNull(stmt)
            stmt?.expression?.testPrefix(operator, right)
        }
    }

    @Test
    fun testBinaryExpressions() {
        data class Data(val input: String, val left: Any, val operator: String, val right: Any)
        listOf(
                Data("5 + 6;", 5, "+", 6),
                Data("5 - 6;", 5, "-", 6),
                Data("5 * 6;", 5, "*", 6),
                Data("5 / 6;", 5, "/", 6),
                Data("5 > 6;", 5, ">", 6),
                Data("5 < 6;", 5, "<", 6),
                Data("5 == 6;", 5, "==", 6),
                Data("5 != 6;", 5, "!=", 6),
                Data("true == true", true, "==", true),
                Data("true != false", true, "!=", false),
                Data("false == false", false, "==", false)
        ).forEach { (input, left, operator, right) ->
            val parser = Parser(input)
            val program = parser.parseProgram()
            parser.checkErrors()

            val stmt = program.statements.singleOrNull() as? ExpressionStatement
            Assert.assertNotNull(stmt)
            stmt?.expression?.testBinary(left, operator, right)
        }
    }

    @Test
    fun testOperatorPrecedenceParsing() {
        listOf(
                "-a * b" to "((-a) * b)",
                "!-a" to "(!(-a))",
                "a + b + c" to "((a + b) + c)",
                "a + b - c" to "((a + b) - c)",
                "a * b * c" to "((a * b) * c)",
                "a * b / c" to "((a * b) / c)",
                "a + b / c" to "(a + (b / c))",
                "a + b * c + d / e - f" to "(((a + (b * c)) + (d / e)) - f)",
                "3 + 4; -5 * 5" to "(3 + 4)((-5) * 5)",
                "5 > 4 == 3 < 4" to "((5 > 4) == (3 < 4))",
                "5 < 4 != 3 > 4" to "((5 < 4) != (3 > 4))",
                "3 + 4 * 5 == 3 * 1 + 4 * 5" to "((3 + (4 * 5)) == ((3 * 1) + (4 * 5)))",
                "true" to "true",
                "false" to "false",
                "3 > 5 == false" to "((3 > 5) == false)",
                "3 < 5 == true" to "((3 < 5) == true)",
                "1 + (2 + 3) + 4" to "((1 + (2 + 3)) + 4)",
                "(5 + 5) * 2" to "((5 + 5) * 2)",
                "2 / (5 + 5)" to "(2 / (5 + 5))",
                "-(5 + 5)" to "(-(5 + 5))",
                "!(true == true)" to "(!(true == true))",
                "a + add(b * c) + d" to "((a + add((b * c))) + d)",
                "add(a, b, 1, 2 * 3, 4 + 5, add(6, 7 * 8))" to "add(a, b, 1, (2 * 3), (4 + 5), add(6, (7 * 8)))",
                "add(a + b + c * d / f + g)" to "add((((a + b) + ((c * d) / f)) + g))",
                "a * [1, 2, 3, 4][b * c] * d" to "((a * ([1, 2, 3, 4][(b * c)])) * d)",
                "add(a * b[2], b[1], 2 * [1, 2][1])" to "add((a * (b[2])), (b[1]), (2 * ([1, 2][1])))"
        ).forEach { (input, expected) ->
            val lexer = Lexer(input)
            val parser = Parser(lexer)
            val program = parser.parseProgram()
            parser.checkErrors()
            Assert.assertEquals(expected, program.text)
        }
    }

    @Test
    fun testIfExpression() {
        val input = "if (x < y) { x }"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val ifExpression = (program.statements.singleOrNull() as? ExpressionStatement)?.expression as? IfExpression
        Assert.assertNotNull(ifExpression)
        ifExpression?.condition?.testBinary("x", "<", "y")

        val consequence = ifExpression?.consequence?.statements?.singleOrNull() as? ExpressionStatement
        Assert.assertNotNull(consequence)
        consequence?.expression?.testLiteral("x")
    }

    @Test
    fun testIfElseExpression() {
        val input = "if (x < y) { x } else { y }"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val ifExpression = (program.statements.singleOrNull() as? ExpressionStatement)?.expression as? IfExpression
        Assert.assertNotNull(ifExpression)
        ifExpression?.condition?.testBinary("x", "<", "y")

        val consequence = ifExpression?.consequence?.statements?.singleOrNull() as? ExpressionStatement
        Assert.assertNotNull(consequence)
        consequence?.expression?.testLiteral("x")

        val alternative = ifExpression?.alternative?.statements?.singleOrNull() as? ExpressionStatement
        Assert.assertNotNull(alternative)
        alternative?.expression?.testLiteral("y")
    }

    @Test
    fun testFunctionExpression() {
        val input = "fn(x, y) { x + y; }"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val fn = (program.statements.singleOrNull() as? ExpressionStatement)?.expression as? FunctionExpression
        Assert.assertNotNull(fn)

        val parameters = fn?.parameters
        Assert.assertEquals(2, parameters?.size)
        parameters?.get(0).testLiteral("x")
        parameters?.get(1).testLiteral("y")

        val bodyStatement = fn?.body?.statements?.firstOrNull() as? ExpressionStatement
        Assert.assertNotNull(bodyStatement)
        bodyStatement?.expression.testBinary("x", "+", "y")
    }

    @Test
    fun testFunctionParameterParsing() {
        listOf(
                "fn() {};" to emptyList(),
                "fn(x) {};" to listOf("x"),
                "fn(x, y, z) {};" to listOf("x", "y", "z")
        ).forEach { (input, expected) ->
            val lexer = Lexer(input)
            val parser = Parser(lexer)
            val program = parser.parseProgram()
            parser.checkErrors()

            val f = (program.statements.firstOrNull() as? ExpressionStatement)?.expression as? FunctionExpression
            Assert.assertNotNull(f)
            Assert.assertEquals(expected.size, f?.parameters?.size)
            expected.zip(f!!.parameters).forEach { it.second.testLiteral(it.first) }
        }
    }

    @Test
    fun testCallExpression() {
        val input = "add(1, 2 * 3, 4 + 5);"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val call = (program.statements.singleOrNull() as? ExpressionStatement)?.expression as? CallExpression
        Assert.assertNotNull(call)
        call?.function.testLiteral("add")

        val arguments = call?.arguments
        Assert.assertNotNull(arguments)
        Assert.assertEquals(3, arguments?.size)
        arguments?.get(0)?.testLiteral(1)
        arguments?.get(1)?.testBinary(2, "*", 3)
        arguments?.get(2)?.testBinary(4, "+", 5)
    }

    @Test
    fun testArrayExpression() {
        val input = "[1, 2 * 3, 4 + 5]"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val a = (program.statements.firstOrNull() as? ExpressionStatement)?.expression as? ArrayExpression
        Assert.assertNotNull(a)
        Assert.assertEquals(3, a?.elements?.size)
        a?.elements?.get(0)?.testLiteral(1)
        a?.elements?.get(1)?.testBinary(2, "*", 3)
        a?.elements?.get(2)?.testBinary(4, "+", 5)
    }

    @Test
    fun testParsingIndexExpression() {
        val input = "myArray[1 + 2]"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val index = (program.statements.firstOrNull() as? ExpressionStatement)?.expression as? IndexExpression
        Assert.assertNotNull(index)
        index?.left?.testLiteral("myArray")
        index?.index?.testBinary(1, "+", 2)
    }

    @Test
    fun testParsingHashWithStringKey() {
        val input = """
            {"one": 1, "two": 2, "three": 3}
        """.trimIndent()
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val hash = (program.statements.firstOrNull() as? ExpressionStatement)?.expression as? HashExpression
        Assert.assertNotNull(hash)
        Assert.assertEquals(3, hash?.pairs?.size)
        listOf("one" to 1, "two" to 2, "three" to 3).forEachIndexed { index, pair ->
            val (expectedKey, expectedValue) = pair
            val (key, value) = hash?.pairs?.get(index) ?: throw AssertionError()
            key.testLiteral(expectedKey)
            value.testLiteral(expectedValue)
        }
    }

    @Test
    fun testParsingEmptyHash() {
        val input = "{}"
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val hash = (program.statements.firstOrNull() as? ExpressionStatement)?.expression as? HashExpression
        Assert.assertNotNull(hash)
        Assert.assertEquals(0, hash?.pairs?.size)
    }

    @Test
    fun testParsingHashWithExpression() {
        val input = """
            {"one": 0 + 1, "two": 10 - 8, "three": 15 / 5}
        """.trimIndent()
        val parser = Parser(input)
        val program = parser.parseProgram()
        parser.checkErrors()

        val hash = (program.statements.firstOrNull() as? ExpressionStatement)?.expression as? HashExpression
        Assert.assertNotNull(hash)
        Assert.assertEquals(3, hash?.pairs?.size)
        listOf(
                "one" to Triple(0, "+", 1),
                "two" to Triple(10, "-", 8),
                "three" to Triple(15, "/", 5)
        ).forEachIndexed { index, pair ->
            val (expectedKey, expectedValue) = pair
            val (key, value) = hash?.pairs?.get(index) ?: throw AssertionError()
            key.testLiteral(expectedKey)
            value.testBinary(expectedValue.first, expectedValue.second, expectedValue.third)
        }
    }

    private fun Parser.checkErrors() {
        val errors = errors()
        if (errors.isEmpty()) return
        errors.forEach { System.err.println(it) }
        Assert.fail()
    }

    private fun Expression?.testPrefix(operator: String, right: Any) {
        val prefix = this as? PrefixExpression
        Assert.assertNotNull(prefix)
        Assert.assertEquals(operator, prefix?.operator)
        prefix?.right?.testLiteral(right)
    }

    private fun Expression?.testBinary(left: Any, operator: String, right: Any) {
        val binary = this as? BinaryExpression
        Assert.assertNotNull(binary)
        Assert.assertEquals(operator, binary?.operator)
        binary?.left?.testLiteral(left)
        binary?.right?.testLiteral(right)
    }

    private fun Expression?.testLiteral(value: Any) {
        val literal = when (value) {
            is String -> (this as? IdentifierExpression) ?: (this as? StringExpression)
            is Int -> this as? IntegerExpression
            is Boolean -> this as? BoolExpression
            else -> {
                Assert.fail("type of exp not handled. got=${this?.text}")
                return
            }
        }
        Assert.assertNotNull(literal)
        Assert.assertEquals("$value", literal?.token?.literal)
        when (literal) {
            is IntegerExpression -> Assert.assertEquals(value, literal.value)
            is BoolExpression -> Assert.assertEquals(value, literal.value)
            is StringExpression -> Assert.assertEquals(value, literal.value)
        }
    }
}