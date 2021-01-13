package cz.tacr.elza.bulkaction.generator.unitid;

import java.util.ArrayList;

import org.apache.commons.lang3.Validate;

/**
 * Part of UnitId
 * 
 * Part is typically:
 * 1
 * 1+1
 * 1-8+2
 * 
 * Generic structure of part: number([+|-]number)
 * 
 *
 */
public class UnitIdPart
        implements Comparable<UnitIdPart> {
    ArrayList<Integer> parts;

    enum Sign {
        PLUS,
        MINUS
    }

    /**
     * Private constructor
     * 
     * Use parse method to build UnitIdPart
     */
    private UnitIdPart() {
        parts = new ArrayList<>();
    }

    private UnitIdPart(UnitIdPart src) {
        parts = new ArrayList<>(src.parts);
    }

    static public UnitIdPart parse(String input) throws UnitIdException {

        UnitIdPart result = new UnitIdPart();
    
        int value = 0;
        int pos = 0;
        Sign sign = null;
        
        while (pos < input.length()) {
            char c = input.charAt(pos);
        
            if (c >= '0' && c <= '9') {
                // first digit have to be nonzero
                if (value == 0 && c == '0') {
                    // unrecognized character
                    throw new UnitIdException("Invalid token: '" + c + "', position: " + (pos + 1));
                }
                value = value * 10 + (c - '0');
            } else 
            // only one sign is allowed
            if (c == '-') {
                result.storePart(value, sign);

                value = 0;
                sign = Sign.MINUS;
            } else 
            if (c == '+') {
                result.storePart(value, sign);

                value = 0;
                sign = Sign.PLUS;
            } else {
                // unrecognized character
                throw new UnitIdException("Invalid token: '"+c+"', position: " + (pos+1) );
            }
            pos++;
        }

        if (value != 0) {
            result.storePart(value, sign);
        } else {
            if (sign != null) {
                throw new UnitIdException("Invalid last token");
            }
        }

        return result;
    }

    private void storePart(int value, Sign sign) throws UnitIdException {
        // First part has to be without sign
        if (sign != null) {
            if (parts.size() == 0) {
                throw new UnitIdException("First part has to be without sign");
            }

            if (sign.equals(Sign.MINUS)) {
                value = -value;
            }
        }
        if (value == 0) {
            throw new UnitIdException("Part cannot be 0");
        }

        parts.add(value);
    }

    public int getParticlesCount() {
        return parts.size();
    }

    public int getParticle(int i) {
        return parts.get(i).intValue();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(parts.get(0));
        for (int pos = 1; pos < parts.size(); pos++) {
            Integer v = parts.get(pos);
            if (v > 0) {
                sb.append('+');
            }
            sb.append(v);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return parts.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != UnitIdPart.class) {
            return false;
        }
        UnitIdPart p = (UnitIdPart) obj;
        // compare arrays
        int sz = parts.size();
        if (sz != p.parts.size()) {
            return false;
        }
        for (int i = 0; i < sz; i++) {
            if (parts.get(i) != p.parts.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 
     * @param particle2
     * @return the value 0 if the argument is equal to this;
     *         a value less than 0 if this is less than the argument;
     *         and a value greater than 0 if this is greater than the argument.
     */
    @Override
    public int compareTo(UnitIdPart particle2) {

        int pos = 0;
        int maxParts = Math.max(parts.size(), particle2.parts.size());
        while (pos < maxParts) {
            int i, i2;
            if (parts.size() <= pos) {
                // value not exists
                i = 0;
            } else {
                i = parts.get(pos);
            }

            if (particle2.getParticlesCount() <= pos) {
                // value not exists
                i2 = 0;
            } else {
                // value exists
                i2 = particle2.getParticle(pos);
            }

            if (i != i2) {
                return (i < i2) ? -1 : 1;
            }
            pos++;
        }
        return 0;
    }

    public UnitIdPart getGreater() {
        UnitIdPart result = new UnitIdPart(this);
        // increment last item
        result.increase();
        return result;
    }

    /**
     * Return lower value
     * 
     * If value == 1 -> return null
     * 
     * @return
     */
    public UnitIdPart getLower() {
        if (parts.size() == 1 && parts.get(0) == 1) {
            return null;
        } else {
            UnitIdPart result = new UnitIdPart(this);

            // decrement last item
            result.decrease();
            return result;
        }
    }

    public UnitIdPart getHigherBase() {
        UnitIdPart result = new UnitIdPart();
        result.parts.add(this.parts.get(0) + 1);
        return result;
    }

    private void decrease() {
        int pos = parts.size() - 1;
        int value = parts.get(pos).intValue();
        value--;
        if (value == 0) {
            value = -1;

            Validate.isTrue(parts.size() > 1);
        }
        parts.set(pos, value);
    }

    private void increase() {
        int pos = parts.size() - 1;
        int value = parts.get(pos).intValue();
        value++;
        if (value == 0) {
            value = 1;
        }
        parts.set(pos, value);
    }

    /**
     * Return lowest possible unit part
     * 
     * Same as parse("1");
     * 
     * @return
     */
    public static UnitIdPart getLowest() {
        UnitIdPart result = new UnitIdPart();
        result.parts.add(1);
        return result;
    }

    public UnitIdPart getWithAdditionalParticle(int i) {
        UnitIdPart result = new UnitIdPart(this);
        result.parts.add(i);
        return result;
    }

    public int countSameParticles(UnitIdPart hiBorder) {
        int i;
        for (i = 0; i < this.parts.size() && i < hiBorder.getParticlesCount(); i++) {
            int p1 = parts.get(i);
            int p2 = hiBorder.parts.get(i);
            if (p1 != p2) {
                return i;
            }
        }
        return i;
    }

    /**
     * Return new object (copy of this] with value at given position
     * 
     * @param pos
     * @param value
     * @return
     */
    public UnitIdPart getWithLastParticle(int pos, int value) {
        Validate.isTrue(value != 0);
        UnitIdPart result = new UnitIdPart();
        // copy particles
        result.parts.addAll(parts.subList(0, pos));
        // add value
        result.parts.add(value);
        return result;
    }

    public UnitIdPart getWithNumParticles(int size) {
        Validate.isTrue(size > 0);

        UnitIdPart result = new UnitIdPart();
        // copy particles
        result.parts.addAll(parts.subList(0, size));
        return result;
    }

    public UnitIdPart addParticle(int value) {
        Validate.isTrue(value != 0);
        // add value
        UnitIdPart result = new UnitIdPart(this);
        result.parts.add(value);
        return result;
    }
}
