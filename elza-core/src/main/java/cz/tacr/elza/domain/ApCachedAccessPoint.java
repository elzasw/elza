package cz.tacr.elza.domain;

//import cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge; TODO hibernate search 6
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.hibernate.annotations.Type;
//import org.hibernate.search.annotations.Analyzer; TODO hibernate search 6
//import org.hibernate.search.annotations.AnalyzerDef;
//import org.hibernate.search.annotations.ClassBridge;
//import org.hibernate.search.annotations.Field;
//import org.hibernate.search.annotations.Indexed;
//import org.hibernate.search.annotations.Store;
//import org.hibernate.search.annotations.TokenFilterDef;
//import org.hibernate.search.annotations.TokenizerDef;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

@Table
//@Indexed TODO hibernate search 6
//@AnalyzerDef(name = "cz",
//        tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
//        filters = {
//                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
//                @TokenFilterDef(factory = ASCIIFoldingFilterFactory.class)
//        })
//@ClassBridge(name = "data",
//        impl = ApCachedAccessPointClassBridge.class,
//        analyzer = @Analyzer(definition = "cz"),
//        store = Store.YES)
//@Analyzer(definition = "cz")
@Entity(name = "ap_cached_access_point")
public class ApCachedAccessPoint {

    // Constants for fulltext indexing
    public static final String DATA = "data";
    public static final String FIELD_ACCESSPOINT_ID = "accessPointId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer cachedAccessPointId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    @GenericField(name = FIELD_ACCESSPOINT_ID, projectable = Projectable.YES)
    private Integer accessPointId;

    @Lob
    //@Type(type = "org.hibernate.type.TextType") TODO hibernate search 6
    @Column
    private String data;

    public Integer getCachedAccessPointId() {
        return cachedAccessPointId;
    }

    public void setCachedAccessPointId(Integer cachedAccessPointId) {
        this.cachedAccessPointId = cachedAccessPointId;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        if (accessPoint != null) {
            this.accessPointId = accessPoint.getAccessPointId();
        }
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
