package cz.tacr.elza.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.repository.NodeRepository;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 18. 1. 2016
 */
@Component
public class ParentNodeIdsBridge implements StringBridge, FieldBridge , ApplicationContextAware {

    @Autowired
    private static NodeRepository nodeRepository;

    @Override
    public String objectToString(Object object) {
        return getValue(object);
    }

    private static String getValue(Object object) {
        // TODO Auto-generated method stub
        return nodeRepository.findOne((Integer.valueOf((String) object)) ).toString();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        nodeRepository = applicationContext.getBean(NodeRepository.class);
    }

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        ArrData arrData = (ArrData) value;
        Integer nodeId = arrData.getItem().getNode().getNodeId();

        String fieldValue = nodeId.toString();
        Field field = new StringField(name, fieldValue, luceneOptions.getStore());
        document.add( field );
    }
}
