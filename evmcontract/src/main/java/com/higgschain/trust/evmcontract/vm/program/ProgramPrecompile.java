/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.higgschain.trust.evmcontract.vm.program;

import com.higgschain.trust.evmcontract.util.ByteUtil;
import com.higgschain.trust.evmcontract.util.RLP;
import com.higgschain.trust.evmcontract.util.RLPList;
import com.higgschain.trust.evmcontract.vm.OpCode;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Anton Nashatyrev on 06.02.2017.
 */
public class ProgramPrecompile {
    private static final int VERSION = 1;

    private Set<Integer> jumpdest = new HashSet<>();

    /**
     * Serialize byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] serialize() {
        byte[][] jdBytes = new byte[jumpdest.size() + 1][];
        int cnt = 0;
        jdBytes[cnt++] = RLP.encodeInt(VERSION);
        for (Integer dst : jumpdest) {
            jdBytes[cnt++] = RLP.encodeInt(dst);
        }

        return RLP.encodeList(jdBytes);
    }

    /**
     * Deserialize program precompile.
     *
     * @param stream the stream
     * @return the program precompile
     */
    public static ProgramPrecompile deserialize(byte[] stream) {
        RLPList l = (RLPList) RLP.decode2(stream).get(0);
        int ver = ByteUtil.byteArrayToInt(l.get(0).getRLPData());
        if (ver != VERSION) {
            return null;
        }
        ProgramPrecompile ret = new ProgramPrecompile();
        for (int i = 1; i < l.size(); i++) {
            ret.jumpdest.add(ByteUtil.byteArrayToInt(l.get(i).getRLPData()));
        }
        return ret;
    }

    /**
     * Compile program precompile.
     *
     * @param ops the ops
     * @return the program precompile
     */
    public static ProgramPrecompile compile(byte[] ops) {
        ProgramPrecompile ret = new ProgramPrecompile();
        for (int i = 0; i < ops.length; ++i) {

            OpCode op = OpCode.code(ops[i]);
            if (op == null) {
                continue;
            }

            if (op.equals(OpCode.JUMPDEST)) {
                ret.jumpdest.add(i);
            }

            if (op.asInt() >= OpCode.PUSH1.asInt() && op.asInt() <= OpCode.PUSH32.asInt()) {
                i += op.asInt() - OpCode.PUSH1.asInt() + 1;
            }
        }
        return ret;
    }

    /**
     * Has jump dest boolean.
     *
     * @param pc the pc
     * @return the boolean
     */
    public boolean hasJumpDest(int pc) {
        return jumpdest.contains(pc);
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        ProgramPrecompile pp = new ProgramPrecompile();
        pp.jumpdest.add(100);
        pp.jumpdest.add(200);
        byte[] bytes = pp.serialize();

        ProgramPrecompile pp1 = ProgramPrecompile.deserialize(bytes);
        System.out.println(pp1.jumpdest);
    }
}
