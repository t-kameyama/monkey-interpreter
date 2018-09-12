import org.junit.Assert
import org.junit.Test
import kotlin.math.exp

class EvaluatorTest {

    @Test
    fun testEvalIntegerExpression() {
        listOf(
                "5" to 5,
                "10" to 10,
                "-5" to -5,
                "-10" to -10,
                "5 + 5 + 5 + 5 - 10" to 10,
                "2 * 2 * 2 * 2 * 2" to 32,
                "-50 + 100 + -50" to 0,
                "5 * 2 + 10" to 20,
                "5 + 2 * 10" to 25,
                "20 + 2 * -10" to 0,
                "50 / 2 * 2 + 10" to 60,
                "2 * (5 + 10)" to 30,
                "3 * 3 * 3 + 10" to 37,
                "3 * (3 * 3) + 10" to 37,
                "(5 + 10 * 2 + 15 / 3) * 2 + -10" to 50
        ).forEach { (input, expected) ->
            testIntegerObject(input, expected)
        }
    }

    @Test
    fun testEvalBoolExpression() {
        listOf(
                "true" to true,
                "false" to false,
                "1 < 2" to true,
                "1 > 2" to false,
                "1 < 1" to false,
                "1 > 1" to false,
                "1 == 1" to true,
                "1 != 1" to false,
                "1 == 2" to false,
                "1 != 2" to true,
                "true == true" to true,
                "false == false" to true,
                "true == false" to false,
                "true != false" to true,
                "false != true" to true,
                "(1 < 2) == true" to true,
                "(1 < 2) == false" to false,
                "(1 > 2) == true" to false,
                "(1 > 2) == false" to true
        ).forEach { (input, expected) ->
            testBoolObject(input, expected)
        }
    }

    @Test
    fun testBangOperator() {
        listOf(
                "!true" to false,
                "!false" to true,
                "!5" to false,
                "!!true" to true,
                "!!false" to false,
                "!!5" to true
        ).forEach { (input, expected) ->
            testBoolObject(input, expected)
        }
    }

    @Test
    fun testIfElseExpression() {
        listOf(
                "if (true) { 10 }" to 10,
                "if (false) { 10 }" to null,
                "if (1) { 10 }" to 10,
                "if (1 < 2) { 10 }" to 10,
                "if (1 > 2) { 10 }" to null,
                "if (1 > 2) { 10 } else { 20 }" to 20,
                "if (1 < 2) { 10 } else { 20 }" to 10
        ).forEach { (input, expected) ->
            if (expected == null) {
                testNullObject(input)
            } else {
                testIntegerObject(input, expected)
            }
        }
    }

    @Test
    fun testReturnStatement() {
        listOf(
                "return 10;" to 10,
                "return 10; 9;" to 10,
                "return 2 * 5; 9;" to 10,
                "9; return 2 * 5; 9;" to 10,
                """
                if (10 > 1) {
                  if (10 > 1) {
                    return 10;
                  }
                  return 1;
                }
                """.trimIndent() to 10
        ).forEach { (input, expected) ->
            testIntegerObject(input, expected)
        }
    }

    @Test
    fun testErrorHandling() {
        listOf(
                "5 + true;" to "type mismatch: INTEGER + BOOLEAN",
                "5 + true; 5;" to "type mismatch: INTEGER + BOOLEAN",
                "-true" to "unknown operator: -BOOLEAN",
                "true + false;" to "unknown operator: BOOLEAN + BOOLEAN",
                "5; true + false; 5" to "unknown operator: BOOLEAN + BOOLEAN",
                "if (10 > 1) { true + false; }" to "unknown operator: BOOLEAN + BOOLEAN",
                """
                if (10 > 1) {
                  if (10 > 1) {
                    return true + false;
                  }
                  return 1;
                }
                """.trimIndent() to "unknown operator: BOOLEAN + BOOLEAN",
                "foobar" to "identifier not found: foobar",
                "\"Hello\" - \"World\"" to "unknown operator: STRING - STRING",
                "{\"name\": \"Monkey\"}[fn(x) { x }];" to "unusable as hash key: FUNCTION"
        ).forEach { (input, expected) ->
            testErrorObject(input, expected)
        }
    }

    @Test
    fun testLetStatement() {
        listOf(
                "let a = 5; a;" to 5,
                "let a = 5 * 5; a;" to 25,
                "let a = 5; let b = a; b;" to 5,
                "let a = 5; let b = a; let c = a + b + 5; c;" to 15
        ).forEach { (input, expected) ->
            testIntegerObject(input, expected)
        }
    }

    @Test
    fun testFunction() {
        val input = "fn(x) { x + 2; };"
        val o = testEval(input) as? Function
        Assert.assertNotNull(o)
        Assert.assertEquals(1, o?.parameters?.size)
        Assert.assertEquals("x", o?.parameters?.firstOrNull()?.value)
        Assert.assertEquals("(x + 2);", o?.body?.text)
    }

    @Test
    fun testFunctionApplication() {
        listOf(
                "let identity = fn(x) { x; }; identity(5);" to 5,
                "let identity = fn(x) { return x; }; identity(5);" to 5,
                "let double = fn(x) { x * 2 }; double(5);" to 10,
                "let add = fn(x, y) { x + y }; add(5, 5);" to 10,
                "let add = fn(x, y) { x + y }; add(5 + 5, add(5, 5));" to 20,
                "fn(x) { x; }(5)" to 5
        ).forEach { (input, expected) ->
            testIntegerObject(input, expected)
        }
    }

    @Test
    fun testClosure() {
        val input = """
            let newAdder = fn(x) {
              fn(y) { x + y };
            };
            let addTwo = newAdder(2);
            addTwo(2);
        """.trimIndent()
        testIntegerObject(input, 4)
    }

    @Test
    fun testEvalStringExpression() {
        val input = "\"Hello World!\""
        val o = testEval(input) as? Str
        Assert.assertNotNull(input, o)
        Assert.assertEquals("Hello World!", o?.value)
    }

    @Test
    fun testStringConcatenation() {
        val input ="""
            "Hello" + " " + "World!
        """.trimIndent()
        val o = testEval(input) as? Str
        Assert.assertNotNull(input, o)
        Assert.assertEquals("Hello World!", o?.value)
    }

    @Test
    fun testBuiltinFunction() {
        listOf(
                """len("")""" to 0,
                """len("four")""" to 4,
                """len("hello world")""" to 11,
                """len(1)""" to "argument to `len` not supported, got INTEGER",
                """len("one", "two")""" to "wrong number of arguments. got=2, want=1",
                "let arr = [1, 2, 3]; len(arr);" to 3,
                "let arr = [1, 2, 3]; first(arr);" to 1,
                "let arr = [1, 2, 3]; rest(arr)[0];" to 2,
                "let arr = [1, 2, 3]; last(arr);" to 3,
                "let arr = [1, 2, 3]; push(arr, 4)[3];" to 4
        ).forEach { (input, expected) ->
            when (expected) {
                is Int -> testIntegerObject(input, expected)
                is String -> testErrorObject(input, expected)
            }
        }
    }

    @Test
    fun testArray() {
        val input = "[1, 2 * 3, 4 + 5]"
        val a = testEval(input) as? Arr
        Assert.assertEquals(3, a?.elements?.size)
        Assert.assertEquals(1, (a?.elements?.get(0) as? Integer)?.value)
        Assert.assertEquals(6, (a?.elements?.get(1) as? Integer)?.value)
        Assert.assertEquals(9, (a?.elements?.get(2) as? Integer)?.value)
    }

    @Test
    fun testArrayIndex() {
        listOf(
                "[1, 2, 3][0]" to 1,
                "[1, 2, 3][1]" to 2,
                "[1, 2, 3][2]" to 3,
                "let i = 0; [1][i];" to 1,
                "[1, 2, 3][1 + 1];" to 3,
                "let myArray = [1, 2, 3]; myArray[2];" to 3,
                "let myArray = [1, 2, 3]; myArray[0] + myArray[1] + myArray[2];" to 6,
                "let myArray = [1, 2, 3]; let i = myArray[0]; myArray[i]" to 2,
                "[1, 2, 3][3]" to null,
                "[1, 2, 3][-1]" to null
        ).forEach { (input, expected) ->
            if (expected != null) testIntegerObject(input, expected) else testNullObject(input)
        }
    }

    @Test
    fun testHash() {
        val input = """
            let two = "two";
            {
              "one": 10 - 9,
              two: 1 + 1,
              "thr" + "ee": 6 / 2,
              4: 4,
              true: 5,
              false: 6
            }
        """.trimIndent()

        val hash = testEval(input) as? Hash
        Assert.assertNotNull(hash)
        Assert.assertEquals(6, hash?.pairs?.size)
        listOf(
                Str("one") to  1,
                Str("two") to  2,
                Str("three") to 3,
                Integer(4) to  4,
                Bool(true)  to  5,
                Bool(false) to  6
        ).forEach { (expectedKey, expectedValue) ->
            val value = hash?.pairs?.get(expectedKey) as? Integer ?: throw AssertionError()
            Assert.assertEquals(expectedValue, value.value)
        }
    }

    @Test
    fun testHashIndex() {
        listOf(
                "{\"foo\": 5}[\"foo\"]" to  5,
                "{\"foo\": 5}[\"bar\"]" to null,
                "let key = \"foo\"; {\"foo\": 5}[key]" to 5,
                "{}[\"foo\"]" to null,
                "{5: 5}[5]" to 5,
                "{true: 5}[true]" to 5,
                "{false: 5}[false]" to 5
        ).forEach { (input, expected) ->
            if (expected == null) {
                testNullObject(input)
            } else {
                testIntegerObject(input, expected)
            }
        }
    }

    private fun testEval(input: String): Object? = eval(Parser(input).parseProgram(), Environment())

    private fun testIntegerObject(input: String, expected: Int) {
        val o = testEval(input) as? Integer
        Assert.assertNotNull(input, o)
        Assert.assertEquals(expected, o?.value)
    }

    private fun testBoolObject(input: String, expected: Boolean) {
        val o = testEval(input) as? Bool
        Assert.assertNotNull(input, o)
        Assert.assertEquals(expected, o?.value)
    }

    private fun testNullObject(input: String) {
        Assert.assertEquals(Null, testEval(input))
    }

    private fun testErrorObject(input: String, expected: String) {
        val error = testEval(input) as? Error
        Assert.assertNotNull(input, error)
        Assert.assertEquals(input, expected, error?.message)

    }
}