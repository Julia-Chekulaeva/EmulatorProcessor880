import java.lang.Exception

class Processor {

    private class ResSigns {
        var c1: Boolean = false
        var z: Boolean = false
        var p: Boolean = false
        var c2: Boolean = false
        var s: Boolean = false

        fun writeSZP(value: UByte) {
            s = value >= 128u
            z = value == 0u.toUByte()
            var sum = 0
            var value1 = value.toInt()
            for (i in 0 until 8) {
                sum += value1 % 2
                value1 /= 2
            }
            p = sum % 2 == 0
        }
    }

    private val resSigns = ResSigns()

    private var stopped = false

    private var SP: Int = 0
    private var PC: Int = 0

    private val mem: MutableList<Pair<UByte, Boolean>> = MutableList(256 * 256) { 0.toUByte() to true }

    private var B: UByte = 0u
    private var C: UByte = 0u
    private var D: UByte = 0u
    private var E: UByte = 0u
    private var H: UByte = 0u
    private var L: UByte = 0u
    private fun M(): UByte = mem[RP("H")].first
    private var A: UByte = 0u

    private val portsIn: Array<UByte> = Array(256) { 0u }
    private val portsOut: Array<UByte> = Array(256) { 0u }
    private val strDigits = Array(7) { StringBuilder(" ".repeat(60)) }

    override fun toString(): String {
        return String.format(
            "BC: %02x %02x\tDE: %02x %02x\tHL: %02x %02x\tM: %02x\tA: %02x\tSP: %04x\tPC: %04x\nCommand: %s",
            B.toInt(), C.toInt(), D.toInt(), E.toInt(), H.toInt(), L.toInt(), M().toInt(), A.toInt(), SP, PC,
            commands[mem[PC].first]?.first ?: "no command"
        ) + if (stopped) "\t| The processor is stopped" else ""
    }

    fun getInPorts() = portsIn

    fun setInPort(ind: Int, value: UByte) {
        portsIn[ind] = value
    }

    fun getOutPorts() = portsOut

    private fun modifyStrArrayHorisontal(indDigit: Int, ind: Int) {
        strDigits[ind][indDigit + 2] = '='
        strDigits[ind][indDigit + 3] = '='
        strDigits[ind][indDigit + 4] = '='
        strDigits[ind][indDigit + 5] = '='
    }

    private fun modifyStrArrayVertical(indDigit: Int, ind1: Int, ind2: Int) {
        strDigits[ind1][indDigit + ind2] = 'H'
        strDigits[ind1 + 1][indDigit + ind2] = 'H'
    }

    private fun writeDigit(indic: Int, codeByte: UByte) {
        var code = codeByte.toInt()
        if (code % 2 == 1)
            modifyStrArrayHorisontal(indic, 0)
        code /= 2
        if (code % 2 == 1)
            modifyStrArrayVertical(indic, 1, 6)
        code /= 2
        if (code % 2 == 1)
            modifyStrArrayVertical(indic, 4, 6)
        code /= 2
        if (code % 2 == 1)
            modifyStrArrayHorisontal(indic, 6)
        code /= 2
        if (code % 2 == 1)
            modifyStrArrayVertical(indic, 4, 1)
        code /= 2
        if (code % 2 == 1)
            modifyStrArrayVertical(indic, 1, 1)
        code /= 2
        if (code % 2 == 1)
            modifyStrArrayHorisontal(indic, 3)
        code /= 2
        if (code % 2 == 1)
            strDigits[6][indic + 8] = '8'
    }

    fun getDigitsDynamic(): List<String> {
        for (i in 0 until 7)
            for (j in 0 until 60)
                strDigits[i][j] = ' '
        if (portsOut[0x0B] == 0x80.toUByte()) {
            val indic = when (portsOut[0x09]) {
                0x01.toUByte() -> 50
                0x02.toUByte() -> 40
                0x04.toUByte() -> 30
                0x08.toUByte() -> 20
                else -> return strDigits.map { it.toString() }
            }
            writeDigit(indic, portsOut[0x08])
        }
        return strDigits.map { it.toString() }
    }

    fun getDigitsStatic(): List<String> {
        for (i in 0 until 7)
            for (j in 0 until 60)
                strDigits[i][j] = ' '
        if (portsOut[0x07] == 0x80.toUByte()) {
            writeDigit(0, portsOut[0x06])
            writeDigit(10, portsOut[0x05])
            writeDigit(20, portsOut[0x04])
        }
        if (portsOut[0x03] == 0x80.toUByte()) {
            writeDigit(30, portsOut[0x02])
            writeDigit(40, portsOut[0x01])
            writeDigit(50, portsOut[0x00])
        }
        return strDigits.map { it.toString() }
    }

    fun resetReg() {
        SP = 0
        PC = 0
        B = 0u
        C = 0u
        D = 0u
        E = 0u
        H = 0u
        L = 0u
        A = 0u
        stopped = false
    }

    fun resetPorts() {
        for (i in 0 until 256) {
            portsIn[i] = 0u
            portsOut[i] = 0u
        }
    }

    private fun RP(nameRP: String) = when (nameRP) {
        "B" -> (B * 256u + C).toInt()
        "D" -> (D * 256u + E).toInt()
        "H" -> (H * 256u + L).toInt()
        "SP" -> SP
        else -> throw Exception("There is no RP named $nameRP")
    }

    fun writeMem(list: List<Int>, address: Int) {
        for ((i, elem) in list.withIndex()) {
            if (elem !in 0 until 256) {
                throw Exception("The instruction code is not in 0..255: its value is $elem")
            }
            val fullAddress = i + address
            mem[fullAddress] = elem.toUByte() to mem[fullAddress].second
        }
    }

    fun readMem(start: Int, end: Int): List<String> {
        arrange()
        return mem.subList(start, end).withIndex().map {
            String.format(
                "%04x :\t%02x |\t%s",
                it.index, it.value.first.toInt(),
                if (it.value.second)
                    commands[it.value.first]?.first ?: "--------"
                else
                    "--data--"
            )
        }
    }

    fun readMemWithoutArrange(start: Int, end: Int): List<String> = mem.subList(start, end).withIndex().map {
        String.format(
            "%04x :\t%02x |\t%s",
            it.index, it.value.first.toInt(),
            commands[it.value.first]?.first ?: "--------"
        )
    }

    private fun arrange() {
        var a = 0
        for (i in 0 until mem.size) {
            if (a > 0) {
                mem[i] = mem[i].first to false
            } else {
                a = commands[mem[i].first]?.second ?: 1
            }
            a--
        }
    }

    private fun writeRP(nameRP: String, value1: UByte, value0: UByte) {
        when (nameRP) {
            "B" -> {
                B = value1
                C = value0
            }
            "D" -> {
                D = value1
                E = value0
            }
            "H" -> {
                H = value1
                L = value0
            }
            "SP" -> SP = value1.toInt() * 256 + value0.toInt()
            else -> throw Exception("There is no RP named $nameRP")
        }
    }

    private fun correctValRP(value: Int): Int = if (value < 0)
        value + 256 * 256 else if (value >= 256 * 256)
            value - 256 * 256 else
            value

    private fun setRP(nameRP: String, value: Int) {
        val correctedValue = correctValRP(value)
        when (nameRP) {
            "B" -> {
                B = (correctedValue / 256).toUByte()
                C = (correctedValue % 256).toUByte()
            }
            "D" -> {
                D = (correctedValue / 256).toUByte()
                E = (correctedValue % 256).toUByte()
            }
            "H" -> {
                H = (correctedValue / 256).toUByte()
                L = (correctedValue % 256).toUByte()
            }
            "SP" -> SP = correctedValue
            else -> throw Exception("There is no RP named $nameRP")
        }
    }

    private fun readRP(nameRP: String): Pair<UByte, UByte> = when (nameRP) {
        "B" -> B to C
        "D" -> D to E
        "H" -> H to L
        "SP" -> (SP / 256).toUByte() to (SP % 256).toUByte()
        else -> throw Exception("There is no RP named $nameRP")
    }

    private fun writeReg(nameReg: String, value: UByte) {
        when (nameReg) {
            "B" -> B = value
            "C" -> C = value
            "D" -> D = value
            "E" -> E = value
            "H" -> H = value
            "L" -> L = value
            "M" -> {
                val address = RP("H")
                mem[address] = value to mem[address].second
            }
            "A" -> A = value
            else -> throw Exception("There is no register named $nameReg")
        }
    }

    private fun readReg(nameReg: String) = when (nameReg) {
        "B" -> B
        "C" -> C
        "D" -> D
        "E" -> E
        "H" -> H
        "L" -> L
        "M" -> mem[RP("H")].first
        "A" -> A
        else -> throw Exception("There is no register named $nameReg")
    }

    fun executeCommand() {
        if (stopped) {
            return
        }
        val command = commands[mem[PC].first]
        if (command == null) {
            PC++
            return
        }
        val description = command.first.split(Regex(", *| +"))
        fun jmp() {
            PC = mem[correctValRP(PC + 1)].first.toInt() + mem[correctValRP(PC + 2)].first.toInt() * 256
        }
        fun call() {
            mem[correctValRP(SP - 1)] = (PC / 256).toUByte() to mem[correctValRP(SP - 1)].second
            mem[correctValRP(SP - 2)] = (PC % 256).toUByte() to mem[correctValRP(SP - 2)].second
            SP = correctValRP(SP - 2)
            PC = mem[correctValRP(PC + 1)].first.toInt() + mem[correctValRP(PC + 2)].first.toInt() * 256
        }
        fun ret() {
            PC = mem[SP].first.toInt() + mem[correctValRP(SP + 1)].first.toInt() * 256
            SP = correctValRP(SP + 2)
        }
        fun add(num: UByte, c: Boolean, compare: Boolean) {
            val prev = A
            val value = A + num + if (c) 1u else 0u
            A = value.toUByte()
            resSigns.writeSZP(A)
            resSigns.c1 = value >= 256u
            resSigns.c2 = A % 16u < num % 16u
            if (compare)
                A = prev
        }
        when (description[0]) {
            "MOV" -> writeReg(description[1], readReg(description[2]))
            "MVI" -> writeReg(description[1], mem[correctValRP(PC + 1)].first)
            "LXI" -> writeRP(description[1], mem[PC + 2].first, mem[correctValRP(PC + 1)].first)
            "LDA" -> A = mem[mem[correctValRP(PC + 2)].first.toInt() * 256 +
                    mem[correctValRP(PC + 1)].first.toInt()].first
            "LDAX" -> A = mem[RP(description[1])].first
            "STA" -> {
                val address = mem[correctValRP(PC + 2)].first.toInt() * 256 +
                        mem[correctValRP(PC + 1)].first.toInt()
                mem[address] = A to mem[address].second
            }
            "STAX" -> {
                val address = RP(description[1])
                mem[address] = A to mem[address].second
            }
            "IN" -> A = portsIn[mem[correctValRP(PC + 1)].first.toInt()]
            "OUT" -> portsOut[mem[correctValRP(PC + 1)].first.toInt()] = A
            "JMP" -> {
                jmp()
                return
            }
            "CALL" -> {
                call()
                return
            }
            "RET" -> {
                ret()
                return
            }
            "PCHL" -> PC = RP("H")
            "RST" -> {
                mem[correctValRP(SP - 1)] = (PC / 256).toUByte() to mem[correctValRP(SP - 1)].second
                mem[correctValRP(SP - 2)] = (PC % 256).toUByte() to mem[correctValRP(SP - 2)].second
                SP = correctValRP(SP - 2)
                PC = description[1].toInt() * 8
            }
            "JNZ" -> {
                if (!resSigns.z) {
                    jmp()
                    return
                }
            }
            "JZ" -> {
                if (resSigns.z) {
                    jmp()
                    return
                }
            }
            "JNC" -> {
                if (!resSigns.c1) {
                    jmp()
                    return
                }
            }
            "JC" -> {
                if (resSigns.c1) {
                    jmp()
                    return
                }
            }
            "JPO" -> {
                if (!resSigns.p) {
                    jmp()
                    return
                }
            }
            "JPE" -> {
                if (resSigns.p) {
                    jmp()
                    return
                }
            }
            "JP" -> {
                if (!resSigns.s) {
                    jmp()
                    return
                }
            }
            "JM" -> {
                if (resSigns.s) {
                    jmp()
                    return
                }
            }
            "CNZ" -> {
                if (!resSigns.z) {
                    call()
                    return
                }
            }
            "CZ" -> {
                if (resSigns.z) {
                    call()
                    return
                }
            }
            "CNC" -> {
                if (!resSigns.c1) {
                    call()
                    return
                }
            }
            "CC" -> {
                if (resSigns.c1) {
                    call()
                    return
                }
            }
            "CPO" -> {
                if (!resSigns.p) {
                    call()
                    return
                }
            }
            "CPE" -> {
                if (resSigns.p) {
                    call()
                    return
                }
            }
            "CP" -> {
                if (!resSigns.s) {
                    call()
                    return
                }
            }
            "CM" -> {
                if (resSigns.s) {
                    call()
                    return
                }
            }
            "RNZ" -> {
                if (!resSigns.z) {
                    ret()
                    return
                }
            }
            "RZ" -> {
                if (resSigns.z) {
                    ret()
                    return
                }
            }
            "RNC" -> {
                if (!resSigns.c1) {
                    ret()
                    return
                }
            }
            "RC" -> {
                if (resSigns.c1) {
                    ret()
                    return
                }
            }
            "RPO" -> {
                if (!resSigns.p) {
                    ret()
                    return
                }
            }
            "RPE" -> {
                if (resSigns.p) {
                    ret()
                    return
                }
            }
            "RP" -> {
                if (!resSigns.s) {
                    ret()
                    return
                }
            }
            "RM" -> {
                if (resSigns.s) {
                    ret()
                    return
                }
            }
            "NOP" -> {}
            "HLT" -> {
                stopped = true
                return
            }
            "ADD" -> add(readReg(description[1]), false, false)
            "ADI" -> add(mem[correctValRP(PC + 1)].first, false, false)
            "ADC" -> add(readReg(description[1]), resSigns.c1, false)
            "ACI" -> add(mem[correctValRP(PC + 1)].first, resSigns.c1, false)
            "SUB" -> add((256u - readReg(description[1])).toUByte(), false, false)
            "SUI" -> add((256u - mem[correctValRP(PC + 1)].first).toUByte(), false, false)
            "SBB" -> add((256u - readReg(description[1])).toUByte(), resSigns.c1, false)
            "SBI" -> add((256u - mem[correctValRP(PC + 1)].first).toUByte(), resSigns.c1, false)
            "CMP" -> add((256u - readReg(description[1])).toUByte(), false, true)
            "CPI" -> add((256u - mem[correctValRP(PC + 1)].first).toUByte(), false, true)
            "INR" -> {
                val value2 = (readReg(description[1]) + 1u).toUByte()
                writeReg(description[1], value2)
                resSigns.writeSZP(value2)
                resSigns.c2 = value2 % 16u == 0u
            }
            "INX" -> {
                setRP(description[1], RP(description[1]) + 1)
            }
            "DCR" -> {
                val value2 = (readReg(description[1]) - 1u).toUByte()
                writeReg(description[1], value2)
                resSigns.writeSZP(value2)
                resSigns.c2 = value2 % 16u != 15u
            }
            "DCX" -> {
                setRP(description[1], RP(description[1]) - 1)
            }
            "DAD" -> {
                val value = RP("H") + RP(description[1])
                setRP("H", value)
                resSigns.c1 = value >= 256 * 256
            }
            "RLC" -> {
                resSigns.c1 = A / 128u == 1u
                A = (A * 2u).toUByte()
            }
            "RRC" -> {
                resSigns.c1 = A % 2u == 1u
                A = (A / 2u).toUByte()
            }
            "RAL" -> {
                resSigns.c1 = A / 128u == 1u
                A = (A * 2u + if (resSigns.c1) 1u else 0u).toUByte()
            }
            "RAR" -> {
                resSigns.c1 = A % 2u == 1u
                A = (A / 2u + if (resSigns.c1) 128u else 0u).toUByte()
            }
            else -> throw Exception("The command ${description[0]} cannot be interpreted")
        }
        PC += command.second
        return
    }

    private val commands: Map<UByte, Pair<String, Int>> = mapOf(
        0x40 to ("MOV B,B"        to 1),
        0x41 to ("MOV B,C"        to 1),
        0x42 to ("MOV B,D"        to 1),
        0x43 to ("MOV B,E"        to 1),
        0x44 to ("MOV B,H"        to 1),
        0x45 to ("MOV B,L"        to 1),
        0x46 to ("MOV B,M"        to 1),
        0x47 to ("MOV B,A"        to 1),

        0x48 to ("MOV C,B"        to 1),
        0x49 to ("MOV C,C"        to 1),
        0x4A to ("MOV C,D"        to 1),
        0x4B to ("MOV C,E"        to 1),
        0x4C to ("MOV C,H"        to 1),
        0x4D to ("MOV C,L"        to 1),
        0x4E to ("MOV C,M"        to 1),
        0x4F to ("MOV C,A"        to 1),

        0x50 to ("MOV D,B"        to 1),
        0x51 to ("MOV D,C"        to 1),
        0x52 to ("MOV D,D"        to 1),
        0x53 to ("MOV D,E"        to 1),
        0x54 to ("MOV D,H"        to 1),
        0x55 to ("MOV D,L"        to 1),
        0x56 to ("MOV D,M"        to 1),
        0x57 to ("MOV D,A"        to 1),

        0x58 to ("MOV E,B"        to 1),
        0x59 to ("MOV E,C"        to 1),
        0x5A to ("MOV E,D"        to 1),
        0x5B to ("MOV E,E"        to 1),
        0x5C to ("MOV E,H"        to 1),
        0x5D to ("MOV E,L"        to 1),
        0x5E to ("MOV E,M"        to 1),
        0x5F to ("MOV E,A"        to 1),

        0x60 to ("MOV H,B"        to 1),
        0x61 to ("MOV H,C"        to 1),
        0x62 to ("MOV H,D"        to 1),
        0x63 to ("MOV H,E"        to 1),
        0x64 to ("MOV H,H"        to 1),
        0x65 to ("MOV H,L"        to 1),
        0x66 to ("MOV H,M"        to 1),
        0x67 to ("MOV H,A"        to 1),

        0x68 to ("MOV L,B"        to 1),
        0x69 to ("MOV L,C"        to 1),
        0x6A to ("MOV L,D"        to 1),
        0x6B to ("MOV L,E"        to 1),
        0x6C to ("MOV L,H"        to 1),
        0x6D to ("MOV L,L"        to 1),
        0x6E to ("MOV L,M"        to 1),
        0x6F to ("MOV L,A"        to 1),

        0x70 to ("MOV M,B"        to 1),
        0x71 to ("MOV M,C"        to 1),
        0x72 to ("MOV M,D"        to 1),
        0x73 to ("MOV M,E"        to 1),
        0x74 to ("MOV M,H"        to 1),
        0x75 to ("MOV M,L"        to 1),
        0x76 to ("MOV M,M"        to 1),
        0x77 to ("MOV M,A"        to 1),

        0x78 to ("MOV A,B"        to 1),
        0x79 to ("MOV A,C"        to 1),
        0x7A to ("MOV A,D"        to 1),
        0x7B to ("MOV A,E"        to 1),
        0x7C to ("MOV A,H"        to 1),
        0x7D to ("MOV A,L"        to 1),
        0x7E to ("MOV A,M"        to 1),
        0x7F to ("MOV A,A"        to 1),

        0x06 to ("MVI B,data8"    to 2),
        0x0E to ("MVI C,data8"    to 2),
        0x16 to ("MVI D,data8"    to 2),
        0x1E to ("MVI E,data8"    to 2),
        0x26 to ("MVI H,data8"    to 2),
        0x2E to ("MVI L,data8"    to 2),
        0x36 to ("MVI M,data8"    to 2),
        0x3E to ("MVI A,data8"    to 2),

        0x01 to ("LXI B,data16"  to 3),
        0x11 to ("LXI D,data16"  to 3),
        0x21 to ("LXI H,data16"  to 3),
        0x31 to ("LXI SP,data16" to 3),

        0x3A to ("LDA data16"    to 3),
        0x0A to ("LDAX B"        to 1),
        0x0A to ("LDAX D"        to 1),

        0x32 to ("STA data16"    to 3),
        0x02 to ("STAX B"        to 1),
        0x12 to ("STAX D"        to 1),

        0xDB to ("IN data8"      to 2),
        0xD3 to ("OUT data8"     to 2),

        0xC3 to ("JMP data16"    to 3),
        0xCD to ("CALL data16"   to 3),
        0xC9 to ("RET"           to 1),
        0xE9 to ("PCHL"          to 1),
        0xC7 to ("RST 0"         to 1),
        0xCF to ("RST 1"         to 1),
        0xD7 to ("RST 2"         to 1),
        0xDF to ("RST 3"         to 1),
        0xE7 to ("RST 4"         to 1),
        0xEF to ("RST 5"         to 1),
        0xF7 to ("RST 6"         to 1),
        0xFF to ("RST 7"         to 1),

        0xC2 to ("JNZ data16"    to 3),
        0xCA to ("JZ data16"     to 3),
        0xD2 to ("JNC data16"    to 3),
        0xDA to ("JC data16"     to 3),
        0xE2 to ("JPO data16"    to 3),
        0xEA to ("JPE data16"    to 3),
        0xF2 to ("JP data16"     to 3),
        0xFA to ("JM data16"     to 3),

        0xC4 to ("CNZ data16"    to 3),
        0xCC to ("CZ data16"     to 3),
        0xD4 to ("CNC data16"    to 3),
        0xDC to ("CC data16"     to 3),
        0xE4 to ("CPO data16"    to 3),
        0xEC to ("CPE data16"    to 3),
        0xF4 to ("CP data16"     to 3),
        0xFC to ("CM data16"     to 3),

        0xC0 to ("RNZ data16"    to 3),
        0xC8 to ("RZ data16"     to 3),
        0xD0 to ("RNC data16"    to 3),
        0xD8 to ("RC data16"     to 3),
        0xE0 to ("RPO data16"    to 3),
        0xE8 to ("RPE data16"    to 3),
        0xF0 to ("RP data16"     to 3),
        0xF8 to ("RM data16"     to 3),

        0xFB to ("EI"            to 1),
        0xF3 to ("DI"            to 1),
        0x00 to ("NOP"           to 1),
        0x76 to ("HLT"           to 1),

        0x80 to ("ADD B"         to 1),
        0x81 to ("ADD C"         to 1),
        0x82 to ("ADD D"         to 1),
        0x83 to ("ADD E"         to 1),
        0x84 to ("ADD H"         to 1),
        0x85 to ("ADD L"         to 1),
        0x86 to ("ADD M"         to 1),
        0x87 to ("ADD A"         to 1),
        0xC6 to ("ADI data8"     to 2),

        0x88 to ("ADC B"         to 1),
        0x89 to ("ADC C"         to 1),
        0x8A to ("ADC D"         to 1),
        0x8B to ("ADC E"         to 1),
        0x8C to ("ADC H"         to 1),
        0x8D to ("ADC L"         to 1),
        0x8E to ("ADC M"         to 1),
        0x8F to ("ADC A"         to 1),
        0xCE to ("ACI data8"     to 2),

        0x90 to ("SUB B"         to 1),
        0x91 to ("SUB C"         to 1),
        0x92 to ("SUB D"         to 1),
        0x93 to ("SUB E"         to 1),
        0x94 to ("SUB H"         to 1),
        0x95 to ("SUB L"         to 1),
        0x96 to ("SUB M"         to 1),
        0x97 to ("SUB A"         to 1),
        0xD6 to ("SUI data8"     to 2),

        0x98 to ("SBB B"         to 1),
        0x99 to ("SBB C"         to 1),
        0x9A to ("SBB D"         to 1),
        0x9B to ("SBB E"         to 1),
        0x9C to ("SBB H"         to 1),
        0x9D to ("SBB L"         to 1),
        0x9E to ("SBB M"         to 1),
        0x9F to ("SBB A"         to 1),
        0xDE to ("SBI data8"     to 2),

        0xB8 to ("CMP B"         to 1),
        0xB9 to ("CMP C"         to 1),
        0xBA to ("CMP D"         to 1),
        0xBB to ("CMP E"         to 1),
        0xBC to ("CMP H"         to 1),
        0xBD to ("CMP L"         to 1),
        0xBE to ("CMP M"         to 1),
        0xBF to ("CMP A"         to 1),
        0xFE to ("CPI data8"     to 2),

        0x04 to ("INR B"         to 1),
        0x0C to ("INR C"         to 1),
        0x14 to ("INR D"         to 1),
        0x1C to ("INR E"         to 1),
        0x24 to ("INR H"         to 1),
        0x2C to ("INR L"         to 1),
        0x34 to ("INR M"         to 1),
        0x3C to ("INR A"         to 1),

        0x03 to ("INX B"         to 1),
        0x13 to ("INX D"         to 1),
        0x23 to ("INX H"         to 1),
        0x33 to ("INX SP"        to 1),

        0x05 to ("DCR B"         to 1),
        0x0D to ("DCR C"         to 1),
        0x15 to ("DCR D"         to 1),
        0x1D to ("DCR E"         to 1),
        0x25 to ("DCR H"         to 1),
        0x2D to ("DCR L"         to 1),
        0x35 to ("DCR M"         to 1),
        0x3D to ("DCR A"         to 1),

        0x0B to ("DCX B"         to 1),
        0x1B to ("DCX D"         to 1),
        0x2B to ("DCX H"         to 1),
        0x3B to ("DCX SP"        to 1),

        0x09 to ("DAD B"         to 1),
        0x19 to ("DAD D"         to 1),
        0x29 to ("DAD H"         to 1),
        0x39 to ("DAD SP"        to 1),

        0x27 to ("DAA"           to 1),

        0xF6 to ("ORI data8"     to 2),
        0x2F to ("CMA"           to 1),

        0x07 to ("RLC"           to 1),
        0x0F to ("RRC"           to 1),
        0x17 to ("RAL"           to 1),
        0x1F to ("RAR"           to 1),

        0x37 to ("STC"           to 1),
        0x3F to ("CMC"           to 1),
    ).map { it.key.toUByte() to it.value }.toMap()
}