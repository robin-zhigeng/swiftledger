package it.unisa.dia.gas.plaf.jpbc.field.z;

import it.unisa.dia.gas.plaf.jpbc.field.base.AbstractField;
import it.unisa.dia.gas.plaf.jpbc.util.math.BigIntegerUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * The type Zr field.
 *
 * @author Angelo De Caro (jpbclib@gmail.com)
 */
public class ZrField extends AbstractField<ZrElement> {
    /**
     * The Order.
     */
    protected BigInteger order;
    /**
     * The Nqr.
     */
    protected ZrElement nqr;
    /**
     * The Fixed length in bytes.
     */
    protected int fixedLengthInBytes;
    /**
     * The Two inverse.
     */
    protected BigInteger twoInverse;

    /**
     * Instantiates a new Zr field.
     *
     * @param order the order
     */
    public ZrField(BigInteger order) {
        this(new SecureRandom(), order, null);
    }

    /**
     * Instantiates a new Zr field.
     *
     * @param random the random
     * @param order  the order
     */
    public ZrField(SecureRandom random, BigInteger order) {
        this(random, order, null);
    }

    /**
     * Instantiates a new Zr field.
     *
     * @param order the order
     * @param nqr   the nqr
     */
    public ZrField(BigInteger order, BigInteger nqr) {
        this(new SecureRandom(), order, nqr);
    }

    /**
     * Instantiates a new Zr field.
     *
     * @param random the random
     * @param order  the order
     * @param nqr    the nqr
     */
    public ZrField(SecureRandom random, BigInteger order, BigInteger nqr) {
        super(random);
        this.order = order;
        this.orderIsOdd = BigIntegerUtils.isOdd(order);

        this.fixedLengthInBytes = (order.bitLength() + 7) / 8;

        this.twoInverse = BigIntegerUtils.TWO.modInverse(order);

        if (nqr != null)
            this.nqr = newElement().set(nqr);
    }


    public ZrElement<ZrField> newElement() {
        return new ZrElement<ZrField>(this);
    }

    public BigInteger getOrder() {
        return order;
    }

    public ZrElement getNqr() {
        if (nqr == null) {
            nqr = newElement();
            do {
                nqr.setToRandom();
            } while (nqr.isSqr());
        }
        
        return nqr.duplicate();
    }

    public int getLengthInBytes() {
        return fixedLengthInBytes;
    }

    public void setFromString(String str) {
        throw new IllegalStateException("Not Implemented yet!");
    }

}