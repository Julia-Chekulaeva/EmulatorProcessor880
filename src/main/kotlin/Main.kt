import java.io.BufferedWriter
import java.io.File
import java.lang.Thread.sleep

fun main(args: Array<String>) {
    val lines = File("src\\main\\resources\\input.txt").readLines().map {
        if (it.contains("#"))
            it.substring(0, it.indexOf("#")).trim()
        else
            it.trim()
    }
    val output = File("src\\main\\resources\\output.txt").bufferedWriter()
    val commands = Array(256 * 256) { 0 }
    var address = 0
    var base = 16
    var commands_count = 1000
    for (line in lines) {
        if (line.matches(Regex("base=\\d+\\s*"))) {
            base = line.substring(5).toInt(10)
        } else if (line.matches(Regex("commands_count=*\\d"))) {
            commands_count = line.substring(14).toInt(10)
        } else {
            for (item in line.split(Regex("\\s+"))) {
                if (item.matches(Regex("\\w+:"))) {
                    address = item.substring(0, item.indexOf(":")).toInt(base)
                } else if (item.matches(Regex("\\w+"))) {
                    commands[address] = item.toInt(base)
                    address++
                }
            }
        }
    }
    val processor = Processor()
    processor.writeMem(commands.toList(), 0)
    processor.readMem(0x0000, 0x0090).forEach {
        printAndWrite(it, output)
    }
    for (i in 0 until commands_count) {
        printAndWrite(processor.toString(), output)
        printAndWrite(".".repeat(60), output)
        processor.getDigitsStatic().forEach {
            printAndWrite(it, output)
        }
        printAndWrite(".".repeat(60), output)
        processor.executeCommand()
    }
    processor.readMem(0x0000, 0x0090).forEach {
        printAndWrite(it, output)
    }
    output.close()
 }

fun printAndWrite(string: String, writer: BufferedWriter) {
    println(string)
    writer.write(string)
    writer.newLine()
}