/*                     __                                               * \
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003, LAMP/EPFL                  **
**  __\ \/ /__/ __ |/ /__/ __ |                                         **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$

package scala.runtime.types;

import scala.Type;
import scala.Array;
import scala.runtime.AtomicReference;
import scala.runtime.IOMap;
import scala.runtime.FNV_Hash;

/**
 * Class modelling a type constructor (this includes non-polymorphic
 * types, which are handled as polymorphic types with zero arguments).
 *
 * @author Michel Schinz
 * @version 1.0
 */

public class TypeConstructor {
    public final static TypeConstructor[] EMPTY_ARRAY =
        new TypeConstructor[0];

    /** Full (qualified) name for this constructor. */
    public final String fullName;

    /** Java class corresponding to this constructor. */
    public Class clazz;

    /** Enclosing class for this type constructor */
    public final Object outer;

    /**
     * Number of invariant (z), contravariant (m) and covariant (p)
     * type arguments.
     */
    public final int zCount, mCount, pCount;

    /**
     * Level of this type in the hierarchy (scala.AnyRef is at 0, its
     * direct children at 1, and so on).
     */
    public final int level;

    /**
     * Indication of triviality: a constructor is trivial iff it has
     * no enclosing class, and no type arguments.
     */
    public final boolean isTrivial;

    /**
     * "Code" to compute the display for an instance of this
     * constructor, based on the display of its parents. This code is
     * structured as follows:
     *
     * n1 p1,1 o1,1 p1,2 o1,2 ... p1,n o1,n  n2 p2,1 ...  nl pl,1 ol,2 ...
     *
     * where all n, p and o are integers, and l is the level of this
     * constructor. ni gives the number of additional entries to add
     * to the display of the super-class at level i. pi gives the
     * index of the parent in which to pick this additional entry, and
     * oi gives the offset of this entry in the parent's display.
     */
    public final int[] displayCode;

    /**
     * "Code" to compute the refinement for an instance of this
     * constructor, based on the refinement of its parents. This code
     * is structured as follows:
     *
     *   n  p1 i1  p2 i2  ...  pn in
     *
     * where n is the total number of refinements, pi is the index of
     * the parent from which refinement i comes (with -1 indicating
     * the current class) and ii is the index of this refinement in
     * the given parent.
     */
    public final int[] refinementCode;

    private final InstantiationMap instMapModule = new InstantiationMap();
    private final AtomicReference/*<InstantiationMap.T>*/ instances =
        new AtomicReference(IOMap.EMPTY);

    private static final ClassLoader loader =
        ClassLoader.getSystemClassLoader();

    public TypeConstructor(int level,
                           String fullName,
                           Object outer,
                           int zCount,
                           int mCount,
                           int pCount,
                           int[] displayCode,
                           int[] refinementCode) {
        this.level = level;
        this.fullName = fullName;
        this.outer = outer;
        this.zCount = zCount;
        this.mCount = mCount;
        this.pCount = pCount;

        this.isTrivial = (outer == null) && (zCount + pCount + mCount == 0);

        this.displayCode = displayCode;
        this.refinementCode = refinementCode;

        try {
            this.clazz = Class.forName(fullName, false, loader);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }

        assert (zCount >= 0) && (mCount >= 0) && (pCount >= 0);
    }

    public String toString() {
        return fullName;
    }

    public ScalaClassType getInstantiation(Type[] args) {
        return instMapModule.get((InstantiationMap.T)instances.get(), args);
    }

    public ScalaClassType instantiate(Type[] args,
                                      ScalaClassType[] parents,
                                      Refinement[] refinement) {
        ScalaClassType tp =
            new ScalaClassType(this, args, parents, refinement);

        try {
            InstantiationMap.T oldMap, newMap;
            do {
                oldMap = (InstantiationMap.T)instances.get();
                newMap = instMapModule.put(oldMap, args, tp);
            } while (!instances.compareAndSet(oldMap, newMap));
        } catch (IOMap.ConflictException e) {
            return (ScalaClassType)e.oldValue;
        }
        return tp;
    }


    //////////////////////////////////////////////////////////////////////

    private static class InstantiationMap extends IOMap {
        public T put(T map, Type[] inst, ScalaClassType value)
            throws ConflictException {
            return super.put(map, Type.hashCode(inst), value);
        }

        public ScalaClassType get(T map, Type[] inst) {
            return (ScalaClassType)super.get(map, Type.hashCode(inst));
        }
    }
}
