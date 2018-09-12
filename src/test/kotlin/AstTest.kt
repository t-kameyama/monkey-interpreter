import org.junit.Assert
import org.junit.Test

class AstTest {

    @Test
    fun testString() {
        // let myVar = anotherVar;
        val program = Program(
            listOf(
                    LetStatement(
                            token = Token(TokenType.LET, "let"),
                            name = IdentifierExpression(Token(TokenType.IDENT, "myVar"), "myVar"),
                            value = IdentifierExpression(Token(TokenType.IDENT, "anotherVar"), "anotherVar")
                    )
            )
        )
        Assert.assertEquals("let myVar = anotherVar;", program.text)
    }
}