package it.unisa.dia.gas.plaf.jpbc.pairing.e;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Point;
import it.unisa.dia.gas.plaf.jpbc.field.curve.CurveField;
import it.unisa.dia.gas.plaf.jpbc.field.gt.GTFiniteElement;
import it.unisa.dia.gas.plaf.jpbc.field.gt.GTFiniteField;
import it.unisa.dia.gas.plaf.jpbc.pairing.map.AbstractPairingMap;

/**
 * The type Type e tate projective miller pairing map.
 *
 * @author Angelo De Caro (jpbclib@gmail.com)
 */
public class TypeETateProjectiveMillerPairingMap extends AbstractPairingMap {
    /**
     * The Pairing.
     */
    protected TypeEPairing pairing;

    /**
     * Instantiates a new Type e tate projective miller pairing map.
     *
     * @param pairing the pairing
     */
    public TypeETateProjectiveMillerPairingMap(TypeEPairing pairing) {
        super(pairing);

        this.pairing = pairing;
    }


    public Element pairing(Point in1, Point in2) {
        Element out = pairing.Fq.newElement();
        Point QR = (Point) in2.duplicate().add(pairing.R);

        e_miller_proj(out, in1, QR, pairing.R);
        out.pow(pairing.phikonr);

        return new GTFiniteElement(this, (GTFiniteField) pairing.getGT(), out);
    }

    public void finalPow(Element element) {
        element.pow(pairing.phikonr);
    }

    /**
     * E miller proj.
     *
     * @param res the res
     * @param P   the p
     * @param QR  the qr
     * @param R   the r
     */
    void e_miller_proj(Element res, Point P, Point QR, Point R) {
        //collate divisions
        int n;
        Element cca = ((CurveField) P.getField()).getA();
        int i;

        Element Zx, Zy;
        Element Px = P.getX();
        Element numx = QR.getX();
        Element numy = QR.getY();
        Element denomx = R.getX();
        Element denomy = R.getY();

        Element a = pairing.Fq.newElement();
        Element b = pairing.Fq.newElement();
        Element c = pairing.Fq.newElement();
        Element e0 = pairing.Fq.newElement();
        Element e1 = pairing.Fq.newElement();

        Element z = pairing.Fq.newOneElement();
        Element z2 = pairing.Fq.newOneElement();

        Element v = pairing.Fq.newOneElement();
        Element vd = pairing.Fq.newOneElement();
        Element v1 = pairing.Fq.newOneElement();
        Element vd1 = pairing.Fq.newOneElement();

        Element e2 = a, e3 = b;

        Point Z = (Point) P.duplicate();
        Zx = Z.getX();
        Zy = Z.getY();
        Point Z1 = (Point) P.getField().newElement();

        n = pairing.exp1;
        for (i = 0; i < n; i++) {
            v.square();
            vd.square();

            do_tangent(v, vd, a, b, c, e0, e1, z, z2, Zx, Zy,  cca, numx, numy, denomx, denomy);
            proj_double(Zx, Zy, e0, e1, e2, e3, z, z2, cca);
            do_vertical(vd, v, Zx, e0, z2, numx, denomx);
        }

        pointToAffine(Zx, Zy, z, z2, e0);

        if (pairing.sign1 < 0) {
            v1.set(vd);
            vd1.set(v);
            do_vertical(vd1, v1, Zx, e0, z2, numx, denomx);
            Z1.set(Z).negate();
        } else {
            v1.set(v);
            vd1.set(vd);
            Z1.set(Z);
        }
        n = pairing.exp2;
        for (; i < n; i++) {
            v.square();
            vd.square();

            do_tangent(v, vd, a, b, c, e0, e1, z, z2, Zx, Zy,  cca, numx, numy, denomx, denomy);
            proj_double(Zx, Zy, e0, e1, e2, e3, z, z2, cca);
            do_vertical(vd, v, Zx, e0, z2, numx, denomx);
        }

        pointToAffine(Zx, Zy, z, z2, e0);


        v.mul(v1);
        vd.mul(vd1);
        do_line(v, vd, Z, Z1, a, b, c, e0, e1, numx, numy, denomx, denomy);
        Z.add(Z1);
        do_vertical(vd, v, Zx, e0, z2, numx, denomx);

        if (pairing.sign0 > 0) {
            do_vertical(v, vd, Px, e0, z2, numx, denomx);
        }

        vd.invert();
        res.set(v).mul(vd);
    }

    /**
     * Proj double.
     *
     * @param Zx  the zx
     * @param Zy  the zy
     * @param e0  the e 0
     * @param e1  the e 1
     * @param e2  the e 2
     * @param e3  the e 3
     * @param z   the z
     * @param z2  the z 2
     * @param cca the cca
     */
    void proj_double(Element Zx, Element Zy, Element e0, Element e1, Element e2, Element e3, Element z, Element z2, Element cca) {
        Element x = Zx;
        Element y = Zy;

        e0.set(x).square();
        e1.set(e0).twice();
        e0.add(e1);
        e1.set(z2).square();
        e1.mul(cca);
        e0.add(e1);



        z.mul(y);
        z.twice();
        z2.set(z).square();

        e2.set(y).square();
        e1.set(x).mul(e2);

        e1.twice().twice();


        e3.set(e1).twice();
        x.set(e0).square();
        x.sub(e3);


        e2.square();
        e2.twice().twice().twice();


        e1.sub(x);
        e0.mul(e1);
        y.set(e0).sub(e2);

        /*
        //e0 = 3x^2 + (cc->a) z^4
        element_square(e0, x);
        //element_mul_si(e0, e0, 3);
        element_double(e1, e0);
        element_add(e0, e0, e1);
        element_square(e1, z2);
        element_mul(e1, e1, cca);
        element_add(e0, e0, e1);

        //z_out = 2 y z
        element_mul(z, y, z);
        //element_mul_si(z, z, 2);
        element_double(z, z);
        element_square(z2, z);

        //e1 = 4 x y^2
        element_square(e2, y);
        element_mul(e1, x, e2);
        //element_mul_si(e1, e1, 4);
        element_double(e1, e1);
        element_double(e1, e1);

        //x_out = e0^2 - 2 e1
        //element_mul_si(e3, e1, 2);
        element_double(e3, e1);
        element_square(x, e0);
        element_sub(x, x, e3);

        //e2 = 8y^4
        element_square(e2, e2);
        //element_mul_si(e2, e2, 8);
        element_double(e2, e2);
        element_double(e2, e2);
        element_double(e2, e2);

        //y_out = e0(e1 - x_out) - e2
        element_sub(e1, e1, x);
        element_mul(e0, e0, e1);
        element_sub(y, e0, e2);
        */
    }

    /**
     * Do tangent.
     *
     * @param e      the e
     * @param edenom the edenom
     * @param a      the a
     * @param b      the b
     * @param c      the c
     * @param e0     the e 0
     * @param e1     the e 1
     * @param z      the z
     * @param z2     the z 2
     * @param Zx     the zx
     * @param Zy     the zy
     * @param cca    the cca
     * @param numx   the numx
     * @param numy   the numy
     * @param denomx the denomx
     * @param denomy the denomy
     */
    void do_tangent(Element e, Element edenom, Element a, Element b, Element c, Element e0, Element e1, Element z, Element z2,
                    Element Zx, Element Zy, Element cca, Element numx, Element numy, Element denomx, Element denomy
    ) {

        a.set(z2).square();
        a.mul(cca);
        b.set(Zx).square();

        e0.set(b).twice();
        b.add(e0);
        a.add(b);
        a.negate();

        e0.set(Zy).twice();
        b.set(e0).mul(z2);
        b.mul(z);

        c.set(Zx).mul(a);
        a.mul(z2);
        e0.mul(Zy);
        c.add(e0).negate();

        e0.set(a).mul(numx);
        e1.set(b).mul(numy);
        e0.add(e1).add(c);
        e.mul(e0);

        e0.set(a).mul(denomx);
        e1.set(b).mul(denomy);
        e0.add(e1).add(c);
        edenom.mul(e0);

        /*
        //a = -(3x^2 + cca z^4)
        //b = 2 y z^3
        //c = -(2 y^2 + x a)
        //a = z^2 a
        element_square(a, z2);
        element_mul(a, a, cca);
        element_square(b, Zx);
        //element_mul_si(b, b, 3);
        element_double(e0, b);
        element_add(b, b, e0);
        element_add(a, a, b);
        element_neg(a, a);

        //element_mul_si(e0, Zy, 2);
        element_double(e0, Zy);
        element_mul(b, e0, z2);
        element_mul(b, b, z);

        element_mul(c, Zx, a);
        element_mul(a, a, z2);
        element_mul(e0, e0, Zy);
        element_add(c, c, e0);
        element_neg(c, c);

        element_mul(e0, a, numx);
        element_mul(e1, b, numy);
        element_add(e0, e0, e1);
        element_add(e0, e0, c);
        element_mul(e, e, e0);

        element_mul(e0, a, denomx);
        element_mul(e1, b, denomy);
        element_add(e0, e0, e1);
        element_add(e0, e0, c);
        element_mul(edenom, edenom, e0);
        */
    }

    /**
     * Do vertical.
     *
     * @param e      the e
     * @param edenom the edenom
     * @param Ax     the ax
     * @param e0     the e 0
     * @param z2     the z 2
     * @param numx   the numx
     * @param denomx the denomx
     */
    void do_vertical(Element e, Element edenom, Element Ax, Element e0, Element z2, Element numx, Element denomx) {
        e0.set(numx).mul(z2);
        e0.sub(Ax);
        e.mul(e0);

        e0.set(denomx).mul(z2);
        e0.sub(Ax);
        edenom.mul(e0);

        /*
        element_mul(e0, numx, z2);
        element_sub(e0, e0, Ax);
        element_mul(e, e, e0);

        element_mul(e0, denomx, z2);
        element_sub(e0, e0, Ax);
        element_mul(edenom, edenom, e0);
        */
    }

    /**
     * Do line.
     *
     * @param e      the e
     * @param edenom the edenom
     * @param A      the a
     * @param B      the b
     * @param a      the a
     * @param b      the b
     * @param c      the c
     * @param e0     the e 0
     * @param e1     the e 1
     * @param numx   the numx
     * @param numy   the numy
     * @param denomx the denomx
     * @param denomy the denomy
     */
    void do_line(Element e, Element edenom, Point A, Point B, Element a, Element b, Element c, Element e0, Element e1,
                 Element numx, Element numy, Element denomx, Element denomy) {

        Element Ax = A.getX();
        Element Ay = A.getY();
        Element Bx = B.getX();
        Element By = B.getY();

        b.set(Bx).sub(Ax);
        a.set(Ay).sub(By);
        c.set(Ax).mul(By);
        e0.set(Ay).mul(Bx);
        c.sub(e0);

        e0.set(a).mul(numx);
        e1.set(b).mul(numy);
        e0.add(e1);
        e0.add(c);
        e.mul(e0);

        e0.set(a).mul(denomx);
        e1.set(b).mul(denomy);
        e0.add(e1).add(c);
        edenom.mul(e0);

        /*
        Element Ax = curve_x_coord(A);
        Element Ay = curve_y_coord(A);
        Element Bx = curve_x_coord(B);
        Element By = curve_y_coord(B);

        element_sub(b, Bx, Ax);
        element_sub(a, Ay, By);
        element_mul(c, Ax, By);
        element_mul(e0, Ay, Bx);
        element_sub(c, c, e0);

        element_mul(e0, a, numx);
        element_mul(e1, b, numy);
        element_add(e0, e0, e1);
        element_add(e0, e0, c);
        element_mul(e, e, e0);

        element_mul(e0, a, denomx);
        element_mul(e1, b, denomy);
        element_add(e0, e0, e1);
        element_add(e0, e0, c);
        element_mul(edenom, edenom, e0);
        */
    }

}
