/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.binaryValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.numberValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.AbstractBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.CommonTypesBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.DocumentBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.ExtendedBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.FlattenedBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.IgnoredAttributeBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.InvalidBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.ListBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.MapBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.PrimitiveTypesBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.RemappedAttributeBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.SecondaryIndexBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.SetBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.SetterAnnotatedBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.SimpleBean;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper.testbeans.SortKeyBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class BeanTableSchemaTest {

    @Test
    public void simpleBean_correctlyAssignsPrimaryPartitionKey() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);
        assertThat(beanTableSchema.tableMetadata().primaryPartitionKey(), is("id"));
    }

    @Test
    public void sortKeyBean_correctlyAssignsSortKey() {
        BeanTableSchema<SortKeyBean> beanTableSchema = BeanTableSchema.create(SortKeyBean.class);
        assertThat(beanTableSchema.tableMetadata().primarySortKey(), is(Optional.of("sort")));
    }

    @Test
    public void simpleBean_hasNoSortKey() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);
        assertThat(beanTableSchema.tableMetadata().primarySortKey(), is(Optional.empty()));
    }

    @Test
    public void simpleBean_hasNoAdditionalKeys() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);
        assertThat(beanTableSchema.tableMetadata().allKeys(), contains("id"));
    }

    @Test
    public void sortKeyBean_hasNoAdditionalKeys() {
        BeanTableSchema<SortKeyBean> beanTableSchema = BeanTableSchema.create(SortKeyBean.class);
        assertThat(beanTableSchema.tableMetadata().allKeys(), containsInAnyOrder("id", "sort"));
    }

    @Test
    public void secondaryIndexBean_definesGsiCorrectly() {
        BeanTableSchema<SecondaryIndexBean> beanTableSchema = BeanTableSchema.create(SecondaryIndexBean.class);

        assertThat(beanTableSchema.tableMetadata().indexPartitionKey("gsi"), is("sort"));
        assertThat(beanTableSchema.tableMetadata().indexSortKey("gsi"), is(Optional.of("attribute")));
    }

    @Test
    public void secondaryIndexBean_definesLsiCorrectly() {
        BeanTableSchema<SecondaryIndexBean> beanTableSchema = BeanTableSchema.create(SecondaryIndexBean.class);

        assertThat(beanTableSchema.tableMetadata().indexPartitionKey("lsi"), is("id"));
        assertThat(beanTableSchema.tableMetadata().indexSortKey("lsi"), is(Optional.of("attribute")));
    }

    @Test
    public void dynamoDbIgnore_propertyIsIgnored() {
        BeanTableSchema<IgnoredAttributeBean> beanTableSchema = BeanTableSchema.create(IgnoredAttributeBean.class);
        IgnoredAttributeBean ignoredAttributeBean = new IgnoredAttributeBean();
        ignoredAttributeBean.setId("id-value");
        ignoredAttributeBean.setIntegerAttribute(123);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(ignoredAttributeBean, false);

        assertThat(itemMap.size(), is(1));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
    }

    @Test
    public void setterAnnotations_alsoWork() {
        BeanTableSchema<SetterAnnotatedBean> beanTableSchema = BeanTableSchema.create(SetterAnnotatedBean.class);
        SetterAnnotatedBean setterAnnotatedBean = new SetterAnnotatedBean();
        setterAnnotatedBean.setId("id-value");
        setterAnnotatedBean.setIntegerAttribute(123);

        assertThat(beanTableSchema.tableMetadata().primaryPartitionKey(), is("id"));

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setterAnnotatedBean, false);
        assertThat(itemMap.size(), is(1));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
    }

    @Test
    public void dynamoDbAttribute_remapsAttributeName() {
        BeanTableSchema<RemappedAttributeBean> beanTableSchema = BeanTableSchema.create(RemappedAttributeBean.class);

        assertThat(beanTableSchema.tableMetadata().primaryPartitionKey(), is("remappedAttribute"));
    }

    @Test
    public void dynamoDbFlatten_correctlyFlattensAttributes() {
        BeanTableSchema<FlattenedBean> beanTableSchema = BeanTableSchema.create(FlattenedBean.class);
        AbstractBean abstractBean = new AbstractBean();
        abstractBean.setAttribute2("two");
        FlattenedBean flattenedBean = new FlattenedBean();
        flattenedBean.setId("id-value");
        flattenedBean.setAttribute1("one");
        flattenedBean.setAbstractBean(abstractBean);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(flattenedBean, false);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("attribute2", stringValue("two")));
    }

    @Test
    public void documentBean_correctlyMapsAttributes() {
        BeanTableSchema<DocumentBean> beanTableSchema = BeanTableSchema.create(DocumentBean.class);
        AbstractBean abstractBean = new AbstractBean();
        abstractBean.setAttribute2("two");
        DocumentBean documentBean = new DocumentBean();
        documentBean.setId("id-value");
        documentBean.setAttribute1("one");
        documentBean.setAbstractBean(abstractBean);

        AttributeValue expectedDocument = AttributeValue.builder()
                                                        .m(singletonMap("attribute2", stringValue("two")))
                                                        .build();

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(documentBean, false);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("abstractBean", expectedDocument));
    }

    @Test
    public void extendedBean_correctlyExtendsAttributes() {
        BeanTableSchema<ExtendedBean> beanTableSchema = BeanTableSchema.create(ExtendedBean.class);
        ExtendedBean extendedBean = new ExtendedBean();
        extendedBean.setId("id-value");
        extendedBean.setAttribute1("one");
        extendedBean.setAttribute2("two");

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(extendedBean, false);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("attribute2", stringValue("two")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidBean_throwsIllegalArgumentException() {
        BeanTableSchema.create(InvalidBean.class);
    }

    @Test
    public void itemToMap_nullAttribute_ignoreNullsTrue() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);
        SimpleBean simpleBean = new SimpleBean();
        simpleBean.setId("id-value");

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(simpleBean, true);

        assertThat(itemMap.size(), is(1));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
    }

    @Test
    public void itemToMap_nullAttribute_ignoreNullsFalse() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);
        SimpleBean simpleBean = new SimpleBean();
        simpleBean.setId("id-value");

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(simpleBean, false);

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("integerAttribute", AttributeValues.nullAttributeValue()));
    }

    @Test
    public void itemToMap_nonNullAttribute() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);
        SimpleBean simpleBean = new SimpleBean();
        simpleBean.setId("id-value");
        simpleBean.setIntegerAttribute(123);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(simpleBean, false);

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("integerAttribute", numberValue(123)));
    }

    @Test
    public void mapToItem_createsItem() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue("id-value"));
        itemMap.put("integerAttribute", numberValue(123));
        SimpleBean expectedBean = new SimpleBean();
        expectedBean.setId("id-value");
        expectedBean.setIntegerAttribute(123);

        SimpleBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result, is(expectedBean));
    }

    @Test
    public void attributeValue_returnsValue() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);
        SimpleBean simpleBean = new SimpleBean();
        simpleBean.setId("id-value");
        simpleBean.setIntegerAttribute(123);

        assertThat(beanTableSchema.attributeValue(simpleBean, "integerAttribute"), is(numberValue(123)));
    }

    @Test
    public void listBean_stringList() {
        BeanTableSchema<ListBean> beanTableSchema = BeanTableSchema.create(ListBean.class);
        ListBean listBean = new ListBean();
        listBean.setId("id-value");
        listBean.setStringList(Arrays.asList("one", "two", "three"));

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(listBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .l(stringValue("one"),
                                                                 stringValue("two"),
                                                                 stringValue("three"))
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("stringList", expectedAttributeValue));

        ListBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(listBean)));
    }

    @Test
    public void listBean_stringListList() {
        BeanTableSchema<ListBean> beanTableSchema = BeanTableSchema.create(ListBean.class);
        ListBean listBean = new ListBean();
        listBean.setId("id-value");
        listBean.setStringListList(Arrays.asList(Arrays.asList("one", "two"), Arrays.asList("three", "four")));

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(listBean, true);

        AttributeValue list1 = AttributeValue.builder().l(stringValue("one"), stringValue("two")).build();
        AttributeValue list2 = AttributeValue.builder().l(stringValue("three"), stringValue("four")).build();
        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .l(list1, list2)
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("stringListList", expectedAttributeValue));

        ListBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(listBean)));
    }

    @Test
    public void setBean_stringSet() {
        BeanTableSchema<SetBean> beanTableSchema = BeanTableSchema.create(SetBean.class);
        SetBean setBean = new SetBean();
        setBean.setId("id-value");
        LinkedHashSet<String> stringSet = new LinkedHashSet<>();
        stringSet.add("one");
        stringSet.add("two");
        stringSet.add("three");
        setBean.setStringSet(stringSet);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .ss("one", "two", "three")
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("stringSet", expectedAttributeValue));

        SetBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(setBean)));
    }

    @Test
    public void setBean_integerSet() {
        BeanTableSchema<SetBean> beanTableSchema = BeanTableSchema.create(SetBean.class);
        SetBean setBean = new SetBean();
        setBean.setId("id-value");
        LinkedHashSet<Integer> integerSet = new LinkedHashSet<>();
        integerSet.add(1);
        integerSet.add(2);
        integerSet.add(3);
        setBean.setIntegerSet(integerSet);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .ns("1", "2", "3")
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("integerSet", expectedAttributeValue));

        SetBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(setBean)));
    }

    @Test
    public void setBean_longSet() {
        BeanTableSchema<SetBean> beanTableSchema = BeanTableSchema.create(SetBean.class);
        SetBean setBean = new SetBean();
        setBean.setId("id-value");
        LinkedHashSet<Long> longSet = new LinkedHashSet<>();
        longSet.add(1L);
        longSet.add(2L);
        longSet.add(3L);
        setBean.setLongSet(longSet);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .ns("1", "2", "3")
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("longSet", expectedAttributeValue));

        SetBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(setBean)));
    }

    @Test
    public void setBean_shortSet() {
        BeanTableSchema<SetBean> beanTableSchema = BeanTableSchema.create(SetBean.class);
        SetBean setBean = new SetBean();
        setBean.setId("id-value");
        LinkedHashSet<Short> shortSet = new LinkedHashSet<>();
        shortSet.add((short)1);
        shortSet.add((short)2);
        shortSet.add((short)3);
        setBean.setShortSet(shortSet);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .ns("1", "2", "3")
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("shortSet", expectedAttributeValue));

        SetBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(setBean)));
    }

    @Test
    public void setBean_byteSet() {
        BeanTableSchema<SetBean> beanTableSchema = BeanTableSchema.create(SetBean.class);
        SetBean setBean = new SetBean();
        setBean.setId("id-value");
        LinkedHashSet<Byte> byteSet = new LinkedHashSet<>();
        byteSet.add((byte)1);
        byteSet.add((byte)2);
        byteSet.add((byte)3);
        setBean.setByteSet(byteSet);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .ns("1", "2", "3")
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("byteSet", expectedAttributeValue));

        SetBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(setBean)));
    }

    @Test
    public void setBean_doubleSet() {
        BeanTableSchema<SetBean> beanTableSchema = BeanTableSchema.create(SetBean.class);
        SetBean setBean = new SetBean();
        setBean.setId("id-value");
        LinkedHashSet<Double> doubleSet = new LinkedHashSet<>();
        doubleSet.add(1.1);
        doubleSet.add(2.2);
        doubleSet.add(3.3);
        setBean.setDoubleSet(doubleSet);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .ns("1.1", "2.2", "3.3")
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("doubleSet", expectedAttributeValue));

        SetBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(setBean)));
    }

    @Test
    public void setBean_floatSet() {
        BeanTableSchema<SetBean> beanTableSchema = BeanTableSchema.create(SetBean.class);
        SetBean setBean = new SetBean();
        setBean.setId("id-value");
        LinkedHashSet<Float> floatSet = new LinkedHashSet<>();
        floatSet.add(1.1f);
        floatSet.add(2.2f);
        floatSet.add(3.3f);
        setBean.setFloatSet(floatSet);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .ns("1.1", "2.2", "3.3")
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("floatSet", expectedAttributeValue));

        SetBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(setBean)));
    }

    @Test
    public void setBean_byteBufferSet() {
        ByteBuffer buffer1 = ByteBuffer.wrap("one".getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer2 = ByteBuffer.wrap("two".getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer3 = ByteBuffer.wrap("three".getBytes(StandardCharsets.UTF_8));

        BeanTableSchema<SetBean> beanTableSchema = BeanTableSchema.create(SetBean.class);
        SetBean setBean = new SetBean();
        setBean.setId("id-value");
        LinkedHashSet<ByteBuffer> byteBufferSet = new LinkedHashSet<>();
        byteBufferSet.add(buffer1);
        byteBufferSet.add(buffer2);
        byteBufferSet.add(buffer3);
        setBean.setByteBufferSet(byteBufferSet);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(setBean, true);

        AttributeValue expectedAttributeValue = AttributeValue.builder()
                                                              .bs(SdkBytes.fromByteBuffer(buffer1),
                                                                  SdkBytes.fromByteBuffer(buffer2),
                                                                  SdkBytes.fromByteBuffer(buffer3))
                                                              .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("byteBufferSet", expectedAttributeValue));

        SetBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(setBean)));
    }

    @Test
    public void mapBean_stringStringMap() {
        BeanTableSchema<MapBean> beanTableSchema = BeanTableSchema.create(MapBean.class);
        MapBean mapBean = new MapBean();
        mapBean.setId("id-value");

        Map<String, String> testMap = new HashMap<>();
        testMap.put("one", "two");
        testMap.put("three", "four");

        mapBean.setStringMap(testMap);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(mapBean, true);

        Map<String, AttributeValue> expectedMap = new HashMap<>();
        expectedMap.put("one", stringValue("two"));
        expectedMap.put("three", stringValue("four"));
        AttributeValue expectedMapValue = AttributeValue.builder()
                                                        .m(expectedMap)
                                                        .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("stringMap", expectedMapValue));

        MapBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(mapBean)));
    }

    @Test
    public void mapBean_nestedStringMap() {
        BeanTableSchema<MapBean> beanTableSchema = BeanTableSchema.create(MapBean.class);
        MapBean mapBean = new MapBean();
        mapBean.setId("id-value");

        Map<String, Map<String, String>> testMap = new HashMap<>();
        testMap.put("five", singletonMap("one", "two"));
        testMap.put("six", singletonMap("three", "four"));

        mapBean.setNestedStringMap(testMap);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(mapBean, true);

        Map<String, AttributeValue> expectedMap = new HashMap<>();
        expectedMap.put("five", AttributeValue.builder().m(singletonMap("one", stringValue("two"))).build());
        expectedMap.put("six", AttributeValue.builder().m(singletonMap("three", stringValue("four"))).build());

        AttributeValue expectedMapValue = AttributeValue.builder()
                                                        .m(expectedMap)
                                                        .build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("nestedStringMap", expectedMapValue));

        MapBean reverse = beanTableSchema.mapToItem(itemMap);
        assertThat(reverse, is(equalTo(mapBean)));
    }

    @Test
    public void commonTypesBean() {
        BeanTableSchema<CommonTypesBean> beanTableSchema = BeanTableSchema.create(CommonTypesBean.class);
        CommonTypesBean commonTypesBean = new CommonTypesBean();
        ByteBuffer binaryLiteral = ByteBuffer.wrap("test-string".getBytes(StandardCharsets.UTF_8));

        commonTypesBean.setId("id-value");
        commonTypesBean.setBooleanAttribute(true);
        commonTypesBean.setIntegerAttribute(123);
        commonTypesBean.setLongAttribute(234L);
        commonTypesBean.setShortAttribute((short) 345);
        commonTypesBean.setByteAttribute((byte) 45);
        commonTypesBean.setDoubleAttribute(56.7);
        commonTypesBean.setFloatAttribute((float) 67.8);
        commonTypesBean.setByteBufferAttribute(binaryLiteral);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(commonTypesBean, true);

        assertThat(itemMap.size(), is(9));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("booleanAttribute", AttributeValue.builder().bool(true).build()));
        assertThat(itemMap, hasEntry("integerAttribute", numberValue(123)));
        assertThat(itemMap, hasEntry("longAttribute", numberValue(234)));
        assertThat(itemMap, hasEntry("shortAttribute", numberValue(345)));
        assertThat(itemMap, hasEntry("byteAttribute", numberValue(45)));
        assertThat(itemMap, hasEntry("doubleAttribute", numberValue(56.7)));
        assertThat(itemMap, hasEntry("floatAttribute", numberValue(67.8)));
        assertThat(itemMap, hasEntry("byteBufferAttribute", binaryValue(binaryLiteral)));

        CommonTypesBean reverse = beanTableSchema.mapToItem(itemMap);

        assertThat(reverse, is(equalTo(commonTypesBean)));
    }

    @Test
    public void primiteTypesBean() {
        BeanTableSchema<PrimitiveTypesBean> beanTableSchema = BeanTableSchema.create(PrimitiveTypesBean.class);
        PrimitiveTypesBean primitiveTypesBean = new PrimitiveTypesBean();

        primitiveTypesBean.setId("id-value");
        primitiveTypesBean.setBooleanAttribute(true);
        primitiveTypesBean.setIntegerAttribute(123);
        primitiveTypesBean.setLongAttribute(234L);
        primitiveTypesBean.setShortAttribute((short) 345);
        primitiveTypesBean.setByteAttribute((byte) 45);
        primitiveTypesBean.setDoubleAttribute(56.7);
        primitiveTypesBean.setFloatAttribute((float) 67.8);

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(primitiveTypesBean, true);

        assertThat(itemMap.size(), is(8));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("booleanAttribute", AttributeValue.builder().bool(true).build()));
        assertThat(itemMap, hasEntry("integerAttribute", numberValue(123)));
        assertThat(itemMap, hasEntry("longAttribute", numberValue(234)));
        assertThat(itemMap, hasEntry("shortAttribute", numberValue(345)));
        assertThat(itemMap, hasEntry("byteAttribute", numberValue(45)));
        assertThat(itemMap, hasEntry("doubleAttribute", numberValue(56.7)));
        assertThat(itemMap, hasEntry("floatAttribute", numberValue(67.8)));

        PrimitiveTypesBean reverse = beanTableSchema.mapToItem(itemMap);

        assertThat(reverse, is(equalTo(primitiveTypesBean)));
    }

    @Test
    public void itemToMap_specificAttributes() {
        BeanTableSchema<CommonTypesBean> beanTableSchema = BeanTableSchema.create(CommonTypesBean.class);
        CommonTypesBean commonTypesBean = new CommonTypesBean();

        commonTypesBean.setId("id-value");
        commonTypesBean.setIntegerAttribute(123);
        commonTypesBean.setLongAttribute(234L);
        commonTypesBean.setFloatAttribute((float) 67.8);

        Map<String, AttributeValue> itemMap =
            beanTableSchema.itemToMap(commonTypesBean, Arrays.asList("longAttribute", "floatAttribute"));

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("longAttribute", numberValue(234)));
        assertThat(itemMap, hasEntry("floatAttribute", numberValue(67.8)));
    }

    @Test
    public void beanClass_returnsCorrectClass() {
        BeanTableSchema<SimpleBean> beanTableSchema = BeanTableSchema.create(SimpleBean.class);

        assertThat(beanTableSchema.beanClass(), is(equalTo(SimpleBean.class)));
    }
}