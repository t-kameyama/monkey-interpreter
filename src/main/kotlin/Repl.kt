import java.util.*

fun main(args: Array<String>) {
    val sc = Scanner(System.`in`)
    val env = Environment()
    print(">> ")
    while (sc.hasNextLine()) {
        val line = sc.nextLine()

        val lexer = Lexer(line)
//        var token = lexer.nextToken()
//        while (token.type != TokenType.EOF) {
//            println(token)
//            token = lexer.nextToken()
//        }

        val parser = Parser(lexer)
        val program = parser.parseProgram()
        val errors = parser.errors()
        if (errors.isNotEmpty()) {
            printParserErrors(errors)
            print(">> ")
            continue
        }
//        println(program.text)

        eval(program, env).also {
            println(it.inspect)
        }

        print(">> ")
    }
}

fun printParserErrors(errors: List<String>) {
    errors.forEach { println("\t$it") }
}
