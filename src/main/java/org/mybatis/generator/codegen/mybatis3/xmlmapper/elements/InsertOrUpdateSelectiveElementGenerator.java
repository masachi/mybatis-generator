/*
 *    Copyright 2006-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.generator.codegen.mybatis3.xmlmapper.elements;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.ListUtilities;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

import java.util.List;

public class InsertOrUpdateSelectiveElementGenerator extends AbstractXmlElementGenerator {

    public InsertOrUpdateSelectiveElementGenerator() {
        super();
    }

    @Override
    public void addElements(XmlElement parentElement) {
        XmlElement answer = buildInitialInsert(introspectedTable.getInsertOrUpdateSelectiveStatementId(),
                introspectedTable.getRules().calculateAllFieldsClass());

        StringBuilder sb = new StringBuilder();

        sb.append("insert into "); //$NON-NLS-1$
        sb.append(introspectedTable.getFullyQualifiedTableNameAtRuntime());
        answer.addElement(new TextElement(sb.toString()));

        XmlElement insertTrimElement = new XmlElement("trim"); //$NON-NLS-1$
        insertTrimElement.addAttribute(new Attribute("prefix", "(")); //$NON-NLS-1$ //$NON-NLS-2$
        insertTrimElement.addAttribute(new Attribute("suffix", ")")); //$NON-NLS-1$ //$NON-NLS-2$
        insertTrimElement.addAttribute(new Attribute("suffixOverrides", ",")); //$NON-NLS-1$ //$NON-NLS-2$
        answer.addElement(insertTrimElement);

        XmlElement valuesTrimElement = new XmlElement("trim"); //$NON-NLS-1$
        valuesTrimElement.addAttribute(new Attribute("prefix", "values (")); //$NON-NLS-1$ //$NON-NLS-2$
        valuesTrimElement.addAttribute(new Attribute("suffix", ")")); //$NON-NLS-1$ //$NON-NLS-2$
        valuesTrimElement.addAttribute(new Attribute("suffixOverrides", ",")); //$NON-NLS-1$ //$NON-NLS-2$
        answer.addElement(valuesTrimElement);

        // on duplicate key update
        answer.addElement(new TextElement("on duplicate key update "));
        XmlElement duplicateUpdateTrimElement = new XmlElement("trim"); //$NON-NLS-1$
        duplicateUpdateTrimElement.addAttribute(new Attribute("prefix", "values (")); //$NON-NLS-1$ //$NON-NLS-2$
        duplicateUpdateTrimElement.addAttribute(new Attribute("suffix", ")")); //$NON-NLS-1$ //$NON-NLS-2$
        duplicateUpdateTrimElement.addAttribute(new Attribute("suffixOverrides", ",")); //$NON-NLS-1$ //$NON-NLS-2$
        answer.addElement(duplicateUpdateTrimElement);

        List<IntrospectedColumn> columns = ListUtilities.removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns());

        for (int i = 0; i < columns.size(); i++) {
            IntrospectedColumn introspectedColumn = columns.get(i);

            if (introspectedColumn.isSequenceColumn()
                    || introspectedColumn.getFullyQualifiedJavaType().isPrimitive()) {
                // if it is a sequence column, it is not optional
                // This is required for MyBatis3 because MyBatis3 parses
                // and calculates the SQL before executing the selectKey

                // if it is primitive, we cannot do a null check
                sb.setLength(0);
                sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
                sb.append(',');
                insertTrimElement.addElement(new TextElement(sb.toString()));

                sb.setLength(0);
                sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
                sb.append(',');
                valuesTrimElement.addElement(new TextElement(sb.toString()));

                // TODO 验证一下什么是sequence column

                continue;
            }

            sb.setLength(0);
            sb.append(introspectedColumn.getJavaProperty());
            sb.append(" != null"); //$NON-NLS-1$
            XmlElement insertNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            insertNotNullElement.addAttribute(new Attribute(
                    "test", sb.toString())); //$NON-NLS-1$

            sb.setLength(0);
            sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            sb.append(',');
            insertNotNullElement.addElement(new TextElement(sb.toString()));
            insertTrimElement.addElement(insertNotNullElement);

            sb.setLength(0);
            sb.append(introspectedColumn.getJavaProperty());
            sb.append(" != null"); //$NON-NLS-1$
            XmlElement valuesNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            valuesNotNullElement.addAttribute(new Attribute("test", sb.toString())); //$NON-NLS-1$

            sb.setLength(0);
            sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
            sb.append(',');
            valuesNotNullElement.addElement(new TextElement(sb.toString()));
            valuesTrimElement.addElement(valuesNotNullElement);

            // on duplicate key update
            sb.setLength(0);
            sb.append(introspectedColumn.getJavaProperty());
            sb.append(" != null"); //$NON-NLS-1$
            XmlElement duplicateKeyNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            duplicateKeyNotNullElement.addAttribute(new Attribute("test", sb.toString())); //$NON-NLS-1$

            sb.setLength(0);
            sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            sb.append(" = values(");
            sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            sb.append(")");
            sb.append(',');
            duplicateKeyNotNullElement.addElement(new TextElement(sb.toString()));
            duplicateUpdateTrimElement.addElement(duplicateKeyNotNullElement);
        }

        if (context.getPlugins().sqlMapInsertSelectiveElementGenerated(answer, introspectedTable)) {
            parentElement.addElement(answer);
        }
    }
}
