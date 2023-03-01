package cz.tacr.elza.search;

import cz.tacr.elza.domain.ArrDescItem;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.repository.NodeRepository;

import static cz.tacr.elza.repository.ExceptionThrow.node;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 18. 1. 2016
 */
@Component
public class ParentNodeIdsBridge implements TypeBridge<ArrDescItem> {

    @Autowired
    private static NodeRepository nodeRepository;

    @Override
    public String objectToString(Object object) {
        return getValue(object);
    }

    private static String getValue(Object object) {
        Integer nodeId = Integer.valueOf((String) object);
        return nodeRepository.findById(nodeId)
                .orElseThrow(node(nodeId)).toString();
    }



    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        ArrDescItem descItem = (ArrDescItem) value;
        Integer nodeId = descItem.getNode().getNodeId();
        String fieldValue = nodeId.toString();
        Field field = new StringField(name, fieldValue, luceneOptions.getStore());
        document.add( field );
    }

    @Override
    public void write(DocumentElement documentElement, ArrDescItem descItem, TypeBridgeWriteContext typeBridgeWriteContext) {
        String name = "";
        Integer nodeId = descItem.getNode().getNodeId();
        String fieldValue = nodeId.toString();
        Field field = new StringField(name, fieldValue, luceneOptions.getStore());
        document.add( field );
    }

}
