/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.flavour.expr.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.teavm.flavour.expr.Evaluator;
import org.teavm.flavour.expr.EvaluatorBuilder;
import org.teavm.flavour.expr.InterpretingEvaluatorBuilder;

/**
 *
 * @author Alexey Andreev
 */
public class EvaluatorTest extends BaseEvaluatorTest {
    TestVars vars;

    @Test
    public void evaluatesIntConstant() {
        IntComputation c = parseExpr(IntComputation.class, "23");
        assertThat(c.compute(), is(23));
    }

    @Test
    public void evaluatesBooleanConstant() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "true");
        assertThat(c.compute(), is(true));
    }

    @Test
    public void evaluatesStringConstant() {
        StringComputation c = parseExpr(StringComputation.class, "'foo'");
        assertThat(c.compute(), is("foo"));
    }

    @Test
    public void evaluatesVariable() {
        IntComputation c = parseExpr(IntComputation.class, "intValue");
        vars.intValue(42);
        assertThat(c.compute(), is(42));
    }

    @Test
    public void evaluatesArithmetics() {
        IntComputation c = parseExpr(IntComputation.class, "intValue - 3");
        vars.intValue(8);
        assertThat(c.compute(), is(5));
    }

    @Test
    public void leftAssociativity() {
        IntComputation c = parseExpr(IntComputation.class, "3 - 2 + 1");
        assertThat(c.compute(), is(2));
    }

    @Test
    public void implicitlyCastsByteToInt() {
        IntComputation c = parseExpr(IntComputation.class, "byteValue - 3");
        vars.byteValue((byte)8);
        assertThat(c.compute(), is(5));
    }

    @Test
    public void implicitlyCastsByteToLong() {
        LongComputation c = parseExpr(LongComputation.class, "longWrapper - byteValue");
        vars.longWrapper(8L);
        vars.byteValue((byte)3);
        assertThat(c.compute(), is(5L));
    }

    @Test
    public void addsNumbers() {
        LongComputation c = parseExpr(LongComputation.class, "longWrapper + byteValue");
        vars.longWrapper(2L);
        vars.byteValue((byte)3);
        assertThat(c.compute(), is(5L));
    }

    @Test
    public void concatenatesStrings() {
        StringComputation c = parseExpr(StringComputation.class, "byteValue + '-' + stringValue + '!' + longWrapper");
        vars.byteValue((byte)1);
        vars.stringValue("x");
        vars.longWrapper(2L);
        assertThat(c.compute(), is("1-x!2"));
    }

    @Test
    public void negatesNumber() {
        IntComputation c = parseExpr(IntComputation.class, "-byteValue");
        vars.byteValue((byte)23);
        assertThat(c.compute(), is(-23));
    }

    @Test
    public void comparesNumbersForEquality() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "byteValue == intValue");

        vars.byteValue((byte)2);
        vars.intValue(2);
        assertThat(c.compute(), is(true));
        vars.intValue(3);
        assertThat(c.compute(), is(false));

        vars.byteValue((byte)-1);
        vars.intValue(-1);
        assertThat(c.compute(), is(true));
    }

    @Test
    public void comparesNumberAndWrapper() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "byteValue == longWrapper");

        vars.byteValue((byte)2);
        vars.longWrapper(2L);
        assertThat(c.compute(), is(true));

        vars.longWrapper(3L);
        assertThat(c.compute(), is(false));
    }

    @Test
    public void comparesNumbers() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "byteValue > intValue");

        vars.byteValue((byte)2);
        vars.intValue(1);
        assertThat(c.compute(), is(true));

        vars.intValue(2);
        assertThat(c.compute(), is(false));

        vars.intValue(3);
        assertThat(c.compute(), is(false));
    }

    @Test
    public void comparesReferences() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "object == longWrapper");

        Long value = Long.valueOf(2);
        vars.longWrapper(value);
        vars.object(value);
        assertThat(c.compute(), is(true));

        vars.object(new Object());
        assertThat(c.compute(), is(false));

        vars.longWrapper(null);
        vars.object(null);
        assertThat(c.compute(), is(true));
    }

    @Test
    public void computesAndOperation() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "object == null and intValue == 3");

        vars.object(null);
        vars.intValue(3);
        assertThat(c.compute(), is(true));

        vars.object(new Object());
        vars.intValue(3);
        assertThat(c.compute(), is(false));

        vars.object(null);
        vars.intValue(2);
        assertThat(c.compute(), is(false));
    }

    @Test
    public void computesOrOperation() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "object == null or intValue == 3");

        vars.object(new Object());
        vars.intValue(2);
        assertThat(c.compute(), is(false));

        vars.object(new Object());
        vars.intValue(3);
        assertThat(c.compute(), is(true));

        vars.object(null);
        vars.intValue(2);
        assertThat(c.compute(), is(true));
    }

    @Test
    public void computesNotOperation() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "not intValue == 3");

        vars.intValue(2);
        assertThat(c.compute(), is(true));

        vars.intValue(3);
        assertThat(c.compute(), is(false));
    }

    @Test
    public void castsLongToInt() {
        IntComputation c = parseExpr(IntComputation.class, "(int)longWrapper");
        vars.longWrapper(23L);
        assertThat(c.compute(), is(23));
    }

    @Test
    public void castsObjectToLong() {
        LongComputation c = parseExpr(LongComputation.class, "(Long)object");
        vars.object(2L);
        assertThat(c.compute(), is(2L));
    }

    @Test
    public void castsObjectToPrimitiveArray() {
        IntComputation c = parseExpr(IntComputation.class, "((int[])object)[0]");
        vars.object(new int[] { 23 });
        assertThat(c.compute(), is(23));
    }

    @Test
    public void evaluatesInstanceOf() {
        BooleanComputation c = parseExpr(BooleanComputation.class, "object instanceof Long");

        vars.object(null);
        assertThat(c.compute(), is(false));

        vars.object("foo");
        assertThat(c.compute(), is(false));

        vars.object(23L);
        assertThat(c.compute(), is(true));
    }

    @Test
    public void evaluatesArraySubscript() {
        ObjectComputation o = parseExpr(ObjectComputation.class, "intArray[0]");
        vars.intArray(new int[] { 23 });
        assertThat(o.compute(), is((Object)23));

        IntComputation c = parseExpr(IntComputation.class, "integerArray[0]");
        vars.integerArray(new Integer[] { 42 });
        assertThat(c.compute(), is(42));

        o = parseExpr(ObjectComputation.class, "stringArray[0]");
        vars.stringArray(new String[] { "foo" });
        assertThat(o.compute(), is((Object)"foo"));
    }

    @Test
    public void evaluatesListSubscript() {
        IntComputation c = parseExpr(IntComputation.class, "integerList[0]");
        vars.integerList(Arrays.asList(23));
        assertThat(c.compute(), is(23));
    }

    @Test
    public void evaluatesMapSubscript() {
        IntComputation c = parseExpr(IntComputation.class, "stringIntMap['k']");
        Map<String, Integer> map = new HashMap<>();
        map.put("k", 23);
        vars.stringIntMap(map);
        assertThat(c.compute(), is(23));
    }

    @Test
    public void evaluatesInvocation() {
        StringComputation c = parseExpr(StringComputation.class, "object.getClass().getName()");

        vars.object(new Object());
        assertThat(c.compute(), is("java.lang.Object"));

        vars.object("foo");
        assertThat(c.compute(), is("java.lang.String"));
    }

    @Test
    public void evaluatesStaticInvocation() {
        IntComputation c = parseExpr(IntComputation.class, "Integer.valueOf('23')");
        assertThat(c.compute(), is(23));
    }

    @Test
    public void resolvesMethod() {
        IntComputation c = parseExpr(IntComputation.class, "foo.bar(2)");
        vars.foo(new Foo(3));
        assertThat(c.compute(), is(5));

        StringComputation sc = parseExpr(StringComputation.class, "foo.bar('x')");
        vars.foo(new Foo(3));
        assertThat(sc.compute(), is("x3"));
    }

    @Test
    public void resolvesGenericMethod() {
        IntComputation c = parseExpr(IntComputation.class, "foo.extract(stringIntMap, 'k')");
        vars.foo(new Foo(0));
        Map<String, Integer> map = new HashMap<>();
        map.put("k", 23);
        vars.stringIntMap(map);
        assertThat(c.compute(), is(23));

        ObjectComputation o = parseExpr(ObjectComputation.class, "foo.extract(stringIntMap, (String)null)");
        vars.foo(new Foo(0));
        vars.stringIntMap(new HashMap<String, Integer>());
        assertThat(o.compute(), is((Object)null));
    }

    @Test
    public void evaluatesProperty() {
        StringComputation c = parseExpr(StringComputation.class, "object.getClass().simpleName");
        vars.object("foo");
        assertThat(c.compute(), is("String"));
    }

    @Test
    public void evaluatesField() {
        IntComputation c = parseExpr(IntComputation.class, "foo.y");
        vars.foo(new Foo(23));
        assertThat(c.compute(), is(23));
    }

    @Test
    public void evaluatesArrayLength() {
        IntComputation c = parseExpr(IntComputation.class, "stringArray.length");
        vars.stringArray(new String[] { "foo", "bar" });
        assertThat(c.compute(), is(2));
    }

    public static final int STATIC_FIELD = 3;

    @Test
    public void evaluatesStaticField() {
        IntComputation c = parseExpr(IntComputation.class, "EvaluatorTest.STATIC_FIELD");
        assertThat(c.compute(), is(3));
    }

    private <T> T parseExpr(Class<T> cls, String str) {
        EvaluatorBuilder builder = new InterpretingEvaluatorBuilder()
                .importPackage("java.lang")
                .importPackage("java.util")
                .importClass(EvaluatorTest.class.getName());
        Evaluator<T, TestVars> e = builder.build(cls, TestVars.class, str);
        vars = e.getVariables();
        return e.getFunction();
    }
}
