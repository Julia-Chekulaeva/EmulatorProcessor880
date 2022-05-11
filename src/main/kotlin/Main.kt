import java.lang.Thread.sleep

fun main(args: Array<String>) {
    //showCommands()
    //processingCommands()
    //outModeDynamic()
    //outModeStatic()
    task2()
}

fun showCommands() {
    val processor = Processor()
    processor.writeMem(List(256) { it }, 0)
    processor.readMemWithoutArrange(0x0000, 0x00FF).forEach {
        println(it)
    }
    println(processor.getOutPorts().withIndex().joinToString("\n") {
        String.format("%02x:\t%02x", it.index, it.value.toInt())
    })
}

fun processingCommands() {
    val processor = Processor()
    processor.writeMem(listOf(0x21, 0x0D, 0x00, 0x06, 0x34, 0x0E, 0x42, 0x78, 0x32, 0x0B, 0x00, 0x00, 0x76), 0)
    processor.readMem(0x0000, 0x0020).forEach {
        println(it)
    }
    for (i in 0 until 10) {
        println(processor)
        processor.executeCommand()
    }
    processor.readMem(0x0000, 0x0020).forEach {
        println(it)
    }
    processor.resetReg()
    println(processor)
}

fun outModeDynamic() {
    val processor = Processor()
    processor.writeMem(listOf(
        0x3E, 0x80, 0xD3, 0x0B,
        0x3E, 0x08, 0xD3, 0x09, 0x3E, 0x4F, 0xD3, 0x08, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x00, 0xD3, 0x08,
        0x3E, 0x04, 0xD3, 0x09, 0x3E, 0xDB, 0xD3, 0x08, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x00, 0xD3, 0x08,
        0x3E, 0x02, 0xD3, 0x09, 0x3E, 0x06, 0xD3, 0x08, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x00, 0xD3, 0x08,
        0x3E, 0x01, 0xD3, 0x09, 0x3E, 0x3F, 0xD3, 0x08, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x00, 0xD3, 0x08,
        0xC3, 0x04, 0x00
    ), 0)
    processor.readMem(0x0000, 0x0020).forEach {
        println(it)
    }
    for (i in 0 until 500) {
        println(processor)
        println(".".repeat(60))
        processor.getDigitsDynamic().forEach {
            println(it)
        }
        println(".".repeat(60))
        processor.executeCommand()
        //sleep(500)
    }
    processor.readMem(0x0000, 0x0060).forEach {
        println(it)
    }
    processor.resetReg()
    println(processor)
}

fun outModeStatic() {
    val processor = Processor()
    processor.writeMem(listOf(
        0x3E, 0x80, 0xD3, 0x07, 0xD3, 0x03,
        0x3E, 0x6D, 0xD3, 0x06,
        0x3E, 0x66, 0xD3, 0x05,
        0x3E, 0xCF, 0xD3, 0x04,
        0x3E, 0x5B, 0xD3, 0x02,
        0x3E, 0x06, 0xD3, 0x01,
        0x3E, 0x3F, 0xD3, 0x00,
        0x76
    ), 0)
    processor.readMem(0x0000, 0x0020).forEach {
        println(it)
    }
    for (i in 0 until 100) {
        println(processor)
        println(".".repeat(60))
        processor.getDigitsDynamic().forEach {
            println(it)
        }
        println(".".repeat(60))
        processor.executeCommand()
    }
    processor.readMem(0x0000, 0x0020).forEach {
        println(it)
    }
    processor.resetReg()
    println(processor)
}

fun task2() {
    val processor = Processor()
    processor.writeMem(listOf(
        0x3E,0x80,0xD3,0x0B,0x26,0x00,0x2E,0x80,0xCD,0x18,0x00,0x2E,0x84,0xCD,0x18,0x00,
        0x2E,0x88,0xCD,0x18,0x00,0xC3,0x06,0x00,0x0E,0x08,0x6B,0x3E,0x01,0x47,0xD3,0x09,
        0x7E,0xD3,0x08,0x3E,0x00,0xD3,0x08,0x2C,0x78,0x07,0xFE,0x10,0xC2,0x1D,0x00,0x0D,0xC2,0x1A,0x00,0xC9
    ), 0)
    processor.writeMem(listOf(0x7C,0x39,0x6E,0x73,0x7C,0x66,0x79,0x73,0x73,0x3F,0x37,0x39), 0x80)
    processor.readMem(0x0000, 0x0090).forEach {
        println(it)
    }
    for (i in 0 until 100) {
        println(processor)
        println(".".repeat(60))
        processor.getDigitsStatic().forEach {
            println(it)
        }
        println(".".repeat(60))
        processor.executeCommand()
        sleep(500)
    }
    processor.readMem(0x0000, 0x0090).forEach {
        println(it)
    }
}