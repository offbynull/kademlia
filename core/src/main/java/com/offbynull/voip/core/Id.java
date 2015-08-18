/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.core;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * An ID between 0 and some pre-defined limit.
 * @author Kasra Faghihi
 */
public final class Id implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BigInteger data;
    private final BigInteger limit;

    /**
     * Constructs a {@link Id} from {@link BigInteger}s.
     * @param data id value
     * @param limit limit value
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code data > limit}, or if either argument is {@code < 0}
     */
    private Id(BigInteger data, BigInteger limit) {
        Validate.notNull(data);
        Validate.notNull(limit);
        Validate.isTrue(data.compareTo(limit) <= 0 && data.signum() >= 0 && limit.signum() >= 0);
        
        this.data = data;
        this.limit = limit;
    }

    /**
     * Constructs a {@link Id} from byte arrays. It's assumed that the byte arrays contain unsigned values and are aligned to byte
     * boundaries (e.g. if you want to use the 6 bits instead of a whole byte (8 bits), make sure the 2 top-most bits are 0).
     * @param data id value
     * @param limit limit value
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code data > limit}
     */
    private Id(byte[] data, byte[] limit) {
        this(new BigInteger(1, data), new BigInteger(1, limit)); 
    }

    /**
     * Constructs a {@link Id} with the limit set to {@code 2^n-1} (a limit where the bits are all 1) and the id set to {@code 2^j-1) (an
     * id where the bits are all 1).
     * @param dataExp number of bits for id value, such that the id value will be {@code 2^j-1}.
     * @param limitExp number of bits in limit, such that the limit will be {@code 2^n-1}.
     * @return created id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code dataExp > limitExp}, or if {@code exp <= 0}
     */
    public static Id createExponent(int dataExp, int limitExp) {
        return create(generatePowerOfTwoLimit(dataExp), limitExp); 
    }

    /**
     * Constructs a {@link Id} with the limit set to {@code 2^n-1} (a limit where the bits are all 1).
     * @param data id value
     * @param exp number of bits in limit, such that the limit will be {@code 2^n-1}.
     * @return created id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code data > 2^exp-1}, or if {@code exp <= 0}
     */
    public static Id create(byte[] data, int exp) {
        return new Id(data, generatePowerOfTwoLimit(exp)); 
    }
    
    /**
     * Constructs a small (31-bit or less) {@link Id} with the limit set to {@code 2^n-1} (a limit where the bits are all 1).
     * @param data id value
     * @param exp number of bits in limit, such that the limit will be {@code 2^n-1}.
     * @return created id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code data > 2^exp-1 || data <= 0}, or if {@code exp <= 0}
     */
    public static Id createSmall(int data, int exp) {
        Validate.isTrue(data >= 0);
        Validate.isTrue(exp <= 31);
        return create(new BigInteger("" + data).toByteArray(), exp); 
    }
    
    /**
     * Generates a limit that's {@code 2^n-1}. Another way to think of it is, generates a limit that successively {@code n = (n << 1) | 1}
     * for {@code n} times -- making sure you have a limit that's value is all 1 bits.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code n = 1, limit = 1 (1b)}<li/>
     * <li>{@code n = 2, limit = 3 (11b)}<li/>
     * <li>{@code n = 4, limit = 7 (111b)}<li/>
     * <li>{@code n = 8, limit = 15 (1111b)}<li/>
     * </ul>
     * @param exp exponent, such that the returned value is {@code 2^exp-1}
     * @return {@code 2^exp-1} as a byte array
     * @throws IllegalArgumentException if {@code exp <= 0}
     */
    private static byte[] generatePowerOfTwoLimit(int exp) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, exp);

        BigInteger val = BigInteger.ONE.shiftLeft(exp).subtract(BigInteger.ONE); // (1 << exp) - 1
        
        return val.toByteArray();
    }

    /**
     * Increments an id.
     * @return this id incremented by 1, wrapped if it exceeds limit
     */
    public Id increment() {
        return add(this, new Id(new byte[] {1}, limit.toByteArray()));
    }

    /**
     * Decrements an id.
     * @return this id decremented by 1, wrapped if it it goes below {@code 0}
     */
    public Id decrement() {
        return subtract(this, new Id(new byte[] {1}, limit.toByteArray()));
    }

    /**
     * Xor two IDs together. The limit of the IDs must match.
     * @param lhs right-hand side
     * @param rhs right-hand side
     * @return {@code lhs ^ rhs}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code lhs} doesn't match the limit from {@code rhs}
     */
    public static Id xor(Id lhs, Id rhs) {
        Validate.notNull(lhs);
        Validate.notNull(rhs);
        Validate.isTrue(lhs.limit.equals(rhs.limit));

        BigInteger xored = lhs.data.xor(rhs.data);
        Id xoredId = new Id(xored, lhs.limit);

        return xoredId;
    }

    /**
     * Adds two IDs together. The limit of the IDs must match.
     * @param lhs right-hand side
     * @param rhs right-hand side
     * @return {@code lhs + rhs}, wrapped if it exceeds the limit
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code lhs} doesn't match the limit from {@code rhs}
     */
    public static Id add(Id lhs, Id rhs) {
        Validate.notNull(lhs);
        Validate.notNull(rhs);
        Validate.isTrue(lhs.limit.equals(rhs.limit));

        BigInteger added = lhs.data.add(rhs.data);
        if (added.compareTo(lhs.limit) > 0) {
            added = added.subtract(lhs.limit).subtract(BigInteger.ONE);
        }

        Id addedId = new Id(added, lhs.limit);

        return addedId;
    }

    /**
     * Subtracts two IDs. The limit of the IDs must match.
     * @param lhs left-hand side
     * @param rhs right-hand side
     * @return {@code lhs - rhs}, wrapped around limit if it goes below {@code 0}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code lhs} doesn't match the limit from {@code rhs}
     */
    public static Id subtract(Id lhs, Id rhs) {
        Validate.notNull(lhs);
        Validate.notNull(rhs);
        Validate.isTrue(lhs.limit.equals(rhs.limit));

        BigInteger subtracted = lhs.data.subtract(rhs.data);
        if (subtracted.signum() == -1) {
            subtracted = subtracted.add(lhs.limit).add(BigInteger.ONE);
        }

        Id subtractedId = new Id(subtracted, lhs.limit);

        return subtractedId;
    }

    /**
     * Compare the position of two IDs, using a base reference point. Another way to think of this is that this method compares two IDs from
     * the view of a certain reference point.
     * <p>
     * <b>Example 1:</b><br>
     * The ID limit is 16<br>
     * {@code lhs = 15}<br>
     * {@code rhs = 2}<br>
     * {@code base = 10}<br>
     * In this case, {@code lhs > rhs}. From {@code base}'s view, {@code lhs} is only 5 nodes away, while {@code rhs} is 8 nodes away.
     * <p>
     * <b>Example 2:</b><br>
     * The ID limit is 16<br>
     * {@code lhs = 9}<br>
     * {@code rhs = 2}<br>
     * {@code base = 10}<br>
     * In this case, {@code rhs < lhs}. From {@code base}'s view, {@code lhs} is 15 nodes away, while {@code rhs} is 8 only nodes away.
     * @param base reference point
     * @param lhs left-hand side
     * @param rhs right-hand side
     * @return -1, 0 or 1 as {@lhs} is less than, equal to, or greater than {@code rhs}.
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code lhs} or {@code rhs} doesn't match the limit from {@code base}
     */
    public static int comparePosition(Id base, Id lhs, Id rhs) {
        Validate.notNull(base);
        Validate.notNull(lhs);
        Validate.notNull(rhs);
        Validate.isTrue(base.limit.equals(lhs.limit) && base.limit.equals(rhs.limit));
        
        Id lhsOffsetId = subtract(lhs, base);
        Id rhsOffsetId = subtract(rhs, base);

        BigInteger rhsOffsetIdNum = rhsOffsetId.getValueAsBigInteger();
        BigInteger lhsOffsetIdNum = lhsOffsetId.getValueAsBigInteger();
        
        return lhsOffsetIdNum.compareTo(rhsOffsetIdNum);
    }

    /**
     * Checks to see if this ID is between two other IDs.
     * @param lower lower ID bound
     * @param lowerInclusive {@code true} if lower ID bound is inclusive, {@code false} if exclusive
     * @param upper upper ID bound
     * @param upperInclusive {@code true} if upper ID bound is inclusive, {@code false} if exclusive
     * @return {@code true} if this ID is between the two specified values
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if ID limits don't match this ID's limit
     */
    public boolean isWithin(Id lower, boolean lowerInclusive, Id upper, boolean upperInclusive) {
        Validate.notNull(lower);
        Validate.notNull(upper);
        Validate.isTrue(limit.equals(lower.limit) && limit.equals(upper.limit));
        
        if (lowerInclusive && upperInclusive) {
            return comparePosition(lower, this, lower) >= 0 && comparePosition(lower, this, upper) <= 0;
        } else if (lowerInclusive) {
            return comparePosition(lower, this, lower) >= 0 && comparePosition(lower, this, upper) < 0;
        } else if (upperInclusive) {
            return comparePosition(lower, this, lower) > 0 && comparePosition(lower, this, upper) <= 0;
        } else {
            return comparePosition(lower, this, lower) > 0 && comparePosition(lower, this, upper) < 0;
        }
    }

    public int getBit(int index) {
        int exp = limit.bitLength();
        Validate.isTrue(index >= 0);
        Validate.isTrue(index < exp);
        
        return data.testBit(index) ? 1 : 0;
    }

    /**
     * Returns this ID's prefix as a {@link BigInteger}.
     * @param bitCount size of prefix in bits 
     * @return this ID as a {@link BigInteger}
     * @throws IllegalArgumentException if {@code bitSize} is 0 or larger than number of bits in id
     */
    public BigInteger getValuePrefixAsBigInteger(int bitCount) {
        int exp = limit.bitLength();
        Validate.isTrue(bitCount >= 0);
        Validate.isTrue(bitCount < exp);
        return data.shiftRight(exp - bitCount);
    }
    
    /**
     * Returns this ID as a {@link BigInteger}.
     * @return this ID as a {@link BigInteger}
     */
    public BigInteger getValueAsBigInteger() {
        return data;
    }

    /**
     * Returns this ID as a byte array.
     * @return this ID as a byte array
     */
    public byte[] getValueAsByteArray() {
        return data.toByteArray();
    }

    /**
     * Returns this ID's limit as a {@link BigInteger}.
     * @return this ID's limit as a {@link BigInteger}
     */
    public BigInteger getLimitAsBigInteger() {
        return limit;
    }

    /**
     * Returns this ID's limit as a byte array.
     * @return this ID's limit as a byte array
     */
    public byte[] getLimitAsByteArray() {
        return limit.toByteArray();
    }

    /**
     * Returns as a exponent, where limit is {@code (2^limit) - 1} (this is guaranteed to always work since this class only allows limits
     * that are pow(2) - 1).
     * @return this ID's limit as a exponent
     */
    public int getLimitAsExponent() {
        return limit.bitLength();
    }
    
    /**
     * Checks if the limit of this id passed in satisfies {@code 2^n-1}. In other words, ensures that all bits making up the limit are
     * {@code 1}.
     * @return {@code true} if limit of this id matches {@code 2^n-1}, {@code false} otherwise.
     */
    private boolean isUseableId() {
        BigInteger limit = getLimitAsBigInteger();
        int bitLength = limit.bitLength();
        
        for (int i = 0; i < bitLength; i++) {
            if (!limit.testBit(i)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Gets the bit size of the limit of the of this id.
     * @return bit length of this id
     */
    public int getBitLength() {
        Validate.isTrue(isUseableId()); // sanity check
        BigInteger limit = this.getLimitAsBigInteger();
        return limit.not().getLowestSetBit();
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.data);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Id other = (Id) obj;
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Id{" + "data=" + data + ", limit=" + limit + '}';
    }
}