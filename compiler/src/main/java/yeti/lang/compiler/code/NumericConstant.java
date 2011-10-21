package yeti.lang.compiler.code;

import yeti.lang.BigNum;
import yeti.lang.IntNum;
import yeti.lang.Num;
import yeti.lang.RatNum;
import yeti.lang.compiler.yeti.type.YetiType;

public final class NumericConstant extends Code implements CodeGen {
    Num num;

    NumericConstant(Num num) {
        setType(YetiType.NUM_TYPE);
        this.num = num;
    }

    protected boolean flagop(int fl) {
        return ((fl & INT_NUM) != 0 && num instanceof IntNum) ||
               (fl & STD_CONST) != 0;
    }

    boolean genInt(Ctx ctx, boolean small) {
        if (!(num instanceof IntNum)) {
            return false;
        }
        long n = num.longValue();
        if (!small) {
            ctx.ldcInsn(new Long(n));
        } else if (n >= (long) Integer.MIN_VALUE &&
                   n <= (long) Integer.MAX_VALUE) {
            ctx.intConst((int) n);
        } else {
            return false;
        }
        return true;
    }

    private static final class Impl extends Code {
        String jtype, sig;
        Object val;

        public void gen(Ctx ctx) {
            ctx.typeInsn(NEW, jtype);
            ctx.insn(DUP);
            ctx.ldcInsn(val);
            ctx.visitInit(jtype, sig);
        }
    }

    public void gen2(Ctx ctx, Code param, int line) {
        ctx.typeInsn(NEW, "yeti/lang/RatNum");
        ctx.insn(DUP);
        RatNum rat = ((RatNum) num).reduce();
        ctx.intConst(rat.numerator());
        ctx.intConst(rat.denominator());
        ctx.visitInit("yeti/lang/RatNum", "(II)V");
    }

    public void gen(Ctx ctx) {
        if (ctx.constants.constants.containsKey(num)) {
            ctx.constant(num, this);
            return;
        }
        if (num instanceof RatNum) {
            ctx.constant(num, new SimpleCode(this, null, YetiType.NUM_TYPE, 0));
            return;
        }
        Impl v = new Impl();
        if (num instanceof IntNum) {
            v.jtype = "yeti/lang/IntNum";
            if (IntNum.__1.compareTo(num) <= 0 &&
                IntNum._9.compareTo(num) >= 0) {
                ctx.fieldInsn(GETSTATIC, v.jtype,
                    IntNum.__1.equals(num) ? "__1" :
                    IntNum.__2.equals(num) ? "__2" : "_" + num,
                    "Lyeti/lang/IntNum;");
                ctx.forceType("yeti/lang/Num");
                return;
            }
            v.val = new Long(num.longValue());
            v.sig = "(J)V";
        } else if (num instanceof BigNum) {
            v.jtype = "yeti/lang/BigNum";
            v.val = num.toString();
            v.sig = "(Ljava/lang/String;)V";
        } else {
            v.jtype = "yeti/lang/FloatNum";
            v.val = new Double(num.doubleValue());
            v.sig = "(D)V";
        }
        v.setType(YetiType.NUM_TYPE);
        ctx.constant(num, v);
    }

    Object valueKey() {
        return num;
    }
}