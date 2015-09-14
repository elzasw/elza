package cz.tacr.elza.ui.controller;

import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartySubtype;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartySubtypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.repository.VersionRepository;
import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kontroler pro testovací data.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 3. 9. 2015
 */
@RestController
@RequestMapping("/api/testingData")
public class TestingDataController {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private DataSource dataSource;

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private RuleManager ruleManager;

    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    private ExternalSourceRepository externalSourceRepository;
    @Autowired
    private FindingAidRepository findingAidRepository;
    @Autowired
    protected ChangeRepository changeRepository;
    @Autowired
    protected LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private PartySubtypeRepository partySubtypeRepository;
    @Autowired
    private RegisterTypeRepository registerTypeRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private VariantRecordRepository variantRecordRepository;
    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;

    private static final int MAX_DEPTH = 4;
    private static final int NODES_IN_LEVEL = 3;
    private static final String FA_NAME = "Testovací Archivní pomůcka";
    private static final String LEVEL_TYPE_ATT_CODE = "ZP2015_LEVEL_TYPE";
    private static final String[] attCodes = {"ZP2015_UNIT_ID",
        "ZP2015_OTHER_ID",
        "ZP2015_SERIAL_NUMBER",
        "ZP2015_TITLE",
        "ZP2015_UNIT_DATE",
        "ZP2015_UNIT_TYPE",
        "ZP2015_STORAGE_ID",
        "ZP2015_ORIGINATOR",
        "ZP2015_UNIT_HIST",
        "ZP2015_POSITION",
        "ZP2015_LEGEND"};

    private static final String DT_UNITID = "UNITID";
    private static final String DT_STRING = "STRING";
    private static final String DT_INT = "INT";
    private static final String DT_UNITDATE = "UNITDATE";
    private static final String DT_PARTY_REF = "PARTY_REF";
    private static final String DT_TEXT = "TEXT";
    private static final String DT_COORDINATES = "COORDINATES";
    private static final String DT_FORMATTED_TEXT = "FORMATTED_TEXT";

    private static final String LEVEL_TYPE_SERIES = "ZP2015_LEVEL_SERIES";
    private static final String LEVEL_TYPE_ITEM = "ZP2015_LEVEL_ITEM";
    private static final String LEVEL_TYPE_PART = "ZP2015_LEVEL_PART";
    private static final String LEVEL_TYPE_FOLDER = "ZP2015_LEVEL_FOLDER";

    private static final int MAX_INT_VALUE = 10000;
    private static final String COORDINATES_VALUE = "x=19.0667003&y=50.0656665";
    private static final String TEXT_VALUE = "Pellentesque pretium lectus id turpis. Curabitur vitae diam non enim vestibulum interdum. Sed convallis magna eu sem. Integer lacinia. Proin pede metus, vulputate nec, fermentum fringilla, vehicula vitae, justo. Maecenas ipsum velit, consectetuer eu lobortis ut, dictum at dui. Proin mattis lacinia justo. Duis condimentum augue id magna semper rutrum. Morbi leo mi, nonummy eget tristique non, rhoncus non leo. Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae. Maecenas ipsum velit, consectetuer eu lobortis ut, dictum at dui. In sem justo, commodo ut, suscipit at, pharetra vitae, orci. Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae. Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus, omnis voluptas assumenda est, omnis dolor repellendus. Nullam eget nisl. Integer rutrum, orci vestibulum ullamcorper ultricies, lacus quam ultricies odio, vitae placerat pede sem sit amet enim. Maecenas ipsum velit, consectetuer eu lobortis ut, dictum at dui. Phasellus et lorem id felis nonummy placerat.";
    private static final String FORMATTED_TEXT_VALUE = "<p>Lorem ipsum dolor sit amet, possim accusam qui cu, melius meliore inermis mei ea. Mel facer bonorum tractatos et, ea per dolore possit. Per te veri munere. Vero movet suavitate ius in, ad graecis dignissim assentior has."
            + "  </p>"
            + "  <p>"
            + " Quis aeque melius nec eu, quo autem euripidis ei. His ea odio atomorum comprehensam. Cum erant dolor ad. Vel dolore feugait dignissim ut. Modo utamur signiferumque sea ea, cu eum equidem torquatos. Dicant alterum concludaturque his ut, has id imperdiet dissentias."
            + "  </p>"
            + "  <p>"
            + " Ei pri stet erant. Ei pri amet agam wisi, mel ad tantas inciderint liberavisse, ex consul sententiae quo. Cu sit legendos qualisque, justo ceteros accommodare qui te. Salutatus consequat te nec, vel ad stet consequat rationibus."
            + "  </p>"
            + "  <p>"
            + " Id quot nostro vix, eam ut eius semper voluptatibus, sea nibh dignissim ne. Mea ad errem legimus hendrerit, scaevola partiendo explicari mel cu. Ei est quem timeam. Ubique graece id cum, mei no partem scaevola posidonium. Postea commodo legimus ea eam, persius splendide at eos."
            + "  </p>"
            + "  <p>"
            + " Cu mel splendide interpretaris. Mutat error no mea, pri ferri dissentiet ex, his cu modo putant conclusionemque. Ex meis timeam nam. Nec nibh mucius lucilius at."
            + "  </p>"
            + "  <p>"
            + " Pro simul quando ea, pri no mollis quaeque delectus, pro ferri nominati cu. Dissentiet philosophia per et, id eam nostrum propriae accommodare. Quo at erat semper contentiones, ne cum populo electram salutandi, vidisse admodum qui cu. Id possit recteque mei, no vis adhuc legendos facilisis. Eum te feugiat mediocrem scribentur, no movet dicunt usu, alterum delectus vel ad."
            + "  </p>"
            + "  <p>"
            + " At brute ornatus pertinax usu, cum no phaedrum elaboraret, duo in nulla populo. Mea eu cibo timeam, mei quando theophrastus eu. Vis esse possit putant ea, ei vix nostrud fierent honestatis. Facete viderer deleniti eum in, nostro minimum eu mea. Petentium gubergren rationibus te duo."
            + "  </p>"
            + "  <p>"
            + " Vix id alii constituam ullamcorper, in ius delectus corrumpit, vix quod veri ornatus an. Agam impetus cu nec. Quo facilis hendrerit mnesarchum in, adipisci intellegam mea ei. Quo ad dicat graecis habemus. Mea summo utamur eu, sed iudico senserit forensibus cu. Molestie repudiandae ne sea, vis te utamur suavitate."
            + "  </p>"
            + "  <p>"
            + " Oratio efficiendi per te. Ea eam nominavi ullamcorper. Ne pri quem nobis audiam, ne vis justo tempor conclusionemque. An suas gubergren usu, et nisl invidunt inciderint ius. Usu minim nullam honestatis ex, mel liber euripidis ea."
            + "  </p>"
            + "  <p>"
            + " Choro senserit postulant et his, eos an mazim primis vidisse, cu ius vero nullam doming. Quis sint partem sed ea, falli mollis philosophia ei eum, sea te malorum detracto conceptam. Ex integre detracto disputationi cum, ius ut voluptaria consectetuer. Eos sumo salutandi prodesset no, quod velit ex sed. Ridens consequat id qui, his an ubique corrumpit comprehensam."
            + "  </p>"
            + "  <p>"
            + " Primis quaeque moderatius usu ea. Ut enim reprimique vix, vis noluisse euripidis referrentur ei. His viris mucius rationibus an. Decore equidem insolens ius ea. Cu quas animal convenire vis, an mel stet cibo."
            + "  </p>"
            + "  <p>"
            + " Dicta verterem ne nec, in cum nihil efficiendi, eu porro maiestatis sit. Ea eros nostro duo, eu sit offendit efficiantur complectitur, et tibique scriptorem eum. Prima vivendum usu ad. Aperiam salutandi mnesarchum ad his. Ex vim fastidii erroribus corrumpit, populo noluisse ius eu. Scripta sensibus ea nam, ei assentior mnesarchum cum, ei harum graece sed."
            + "  </p>"
            + "  <p>"
            + " Ne solet facilisis iudicabit pri, oratio consetetur vel et, in solum menandri mnesarchum mei. At decore exerci duo, id omittam mediocritatem nec. Elitr nobis an usu, mei choro veritus deserunt at. Has elit causae lucilius eu. Fierent repudiare instructior ei mei."
            + "  </p>"
            + "  <p>"
            + " Eu adhuc aeque expetendis vis, mea pericula forensibus et. Ridens abhorreant constituto ut quo, ea quo decore argumentum voluptatibus, aeque habemus ut per. Ei ius novum efficiantur. Ad mel voluptatum ullamcorper. Cum ut dicat iusto reprehendunt. Movet inermis theophrastus his cu. Sit eius primis theophrastus te, populo mucius pri ex, ei pro dicit eripuit."
            + "  </p>"
            + "  <p>"
            + " His ut dico dolorum partiendo, his sint liber ei. Quando deseruisse temporibus mea no, cu eius menandri assentior sea. Option menandri evertitur sea te. Ignota perpetua explicari eu eos. Pro eu nominati democritum, iriure impetus imperdiet est an."
            + "  </p>"
            + "  <p>"
            + " Et laudem utamur has. Mei id homero dolorem, vix cu primis vivendo instructior. Illud ipsum assum mei ei, ad prima doming facilisi usu, sed cu periculis splendide deseruisse. Te civibus urbanitas cum, eu adipiscing inciderint sit, pro mentitum laboramus sadipscing ad."
            + "  </p>"
            + "  <p>"
            + " Dolore quaeque eu vim, ad dicant apeirian necessitatibus mei. Ne nibh ferri insolens mei, porro malis ea vix. Quem possim necessitatibus mei in. Id pro perfecto incorrupte. At impetus veritus pertinacia per, has vide persius et. In ubique volutpat pericula pri, mel veri civibus at."
            + "  </p>"
            + "  <p>"
            + " Cu appareat facilisi electram vix, ea vel saepe lobortis. Primis aperiam pri ex, ea novum graeci nam. Nibh iuvaret reprehendunt ad pri. Eum ea omnesque vituperata. Usu ponderum sapientem pertinacia cu, eos quidam adolescens ad. Cu mea alii oblique habemus, labores laboramus ne mel. Duo munere epicurei adolescens in, an has consequat temporibus."
            + "  </p>"
            + "  <p>"
            + " Mutat labore signiferumque et duo, ex ius persius inciderint, case persecuti est an. Veniam urbanitas no his. An nibh detraxit assentior mea, ex nec mentitum deleniti definitionem. Ne pro commune sapientem patrioque, debet consequat nam ea. Evertitur neglegentur ne mei, te usu aliquam legendos delicatissimi. Illud graeci deleniti cum ei."
            + "  </p>"
            + "  <p>"
            + " Sea meis fugit dicat ne, ius solum iusto ubique ut. Has mentitum comprehensam ei, ex suas case assueverit vix, amet eloquentiam referrentur te eum. At impetus graecis eum, ex vix aliquam docendi. Nam mutat accusam an, sit at nisl assueverit. Has dicam epicuri accommodare eu, cum zril minimum prodesset at. Vero harum euripidis ex pro, tantas dicunt salutandi cum eu. In eum clita salutatus assueverit, vim prodesset repudiandae disputationi et."
            + "  </p>"
            + "  <p>"
            + " In usu novum labore. Cu augue nihil dolorum cum, esse semper nec eu. Eu nobis putent nostrum per, in prima erroribus assueverit per. Ut vel novum intellegebat, ad eum aliquip blandit sententiae. Feugait nostrum intellegam te sea. Te viderer efficiendi interpretaris sed. Porro hendrerit adversarium has ei."
            + "  </p>"
            + "  <p>"
            + " At meliore voluptaria pro, alii ceteros eam an. Cu splendide persequeris vim, at mea nonumy dictas. Ei mel hinc harum, an vis mandamus conclusionemque. Eu purto debet cum, doctus democritum eum in, eos decore exerci in. Nec ei essent nominati mediocrem."
            + "  </p>"
            + "  <p>"
            + " Ne eam consequat percipitur scriptorem, sed docendi disputando ex. Invenire adipiscing sadipscing vix ne, te nullam appetere instructior nam. Alii oratio maiestatis in usu, ullum eleifend ut pri, sed diam debitis disputando an. Tale aliquid praesent te sed, ea cum veri ferri sapientem."
            + "  </p>"
            + "  <p>"
            + " Vix dicat urbanitas interesset ad. Probo iisque theophrastus quo no, ea vis mazim debitis signiferumque. Te sapientem facilisis qui. Epicurei laboramus consequat in cum. Id odio veri dicit quo. Tibique intellegam te est."
            + "  </p>"
            + "  <p>"
            + " Nobis liberavisse pri at. Has id tantas nusquam phaedrum, sea ex luptatum recteque. Mel te mucius partiendo. Ut pro minim vituperata sadipscing. Labitur deserunt gubergren eu vel."
            + "  </p>"
            + "  <p>"
            + " Possim nominavi nominati mel in, sit ea rebum forensibus, quo ea dolore moderatius. Mel eu aliquip accumsan accusata. Minimum voluptaria te pro. Eu sonet luptatum invenire mei, ad vim doming nominavi molestiae, cu usu atqui nobis scripta. Labores mnesarchum voluptatibus ne eum, est etiam imperdiet te, quidam meliore ei nec."
            + "  </p>"
            + "  <p>"
            + " Expetenda vituperata ei has, nam ad semper assentior constituto. Mutat noluisse cu sea, has zril splendide vulputate eu. Vim ex erant deleniti. At saepe adipisci consectetuer vim. No qui eloquentiam theophrastus intellegebat, ne eam purto praesent voluptatum. Sit in docendi perfecto ocurreret."
            + "  </p>"
            + "  <p>"
            + " Vis no causae impedit perfecto, ut mea essent necessitatibus, libris menandri cotidieque ut ius. Illud gloriatur inciderint nam ex, unum denique neglegentur vim no. Soleat iuvaret minimum eu pri, ne nihil saperet comprehensam vix, eos suscipit quaestio facilisis te. Ut qui assentior signiferumque, et zril contentiones has. Pro probo malis laoreet an, evertitur honestatis ne quo. At nostro tractatos reformidans cum, ei dicam expetendis per."
            + "  </p>"
            + "  <p>"
            + " Pertinacia constituam mei id, mei ex omnes aliquid. Option recteque mediocrem ut vis, nec ad discere iracundia evertitur. Minimum splendide id vix. Sed primis ornatus perfecto cu, mea ea aliquip concludaturque. Vim nihil maiorum scribentur ex. Mel no scripserit concludaturque."
            + "  </p>"
            + "  <p>"
            + " Eam et falli impetus assentior, ut melius voluptua has. Tantas suscipit adversarium ut nec, ea est dicat quaeque ceteros. Ea vix homero timeam, aperiri necessitatibus ea eam. Verear denique pri at. Quo sonet imperdiet scripserit no. Per te quidam dissentiet."
            + "  </p>"
            + "  <p>"
            + " Omnis similique vim in, usu suas consequuntur ne. Ei eos alia magna gloriatur, regione oportere sit at. Et scribentur intellegebat concludaturque vel, dicta quidam feugait nam no. Nec legimus mentitum inimicus te, maiorum omittantur ad est. Quo ex error quaerendum complectitur, nam referrentur vituperatoribus cu. Viris pertinax mea no, denique molestie mea ea, noster aliquid petentium et vix."
            + "  </p>"
            + "  <p>"
            + " Nec te omittam accumsan mnesarchum. Pri debet ocurreret dissentiunt id, ex omittam intellegat mnesarchum his. Iisque labores an mel, ei has eros similique, quem movet adversarium ex pro. Ius ei etiam propriae adversarium."
            + "  </p>"
            + "  <p>"
            + " Ullum consulatu id vim. Eum ut bonorum evertitur, in eum partem concludaturque. Ad sonet laudem qui, fastidii pericula ut eam, scaevola urbanitas id eam. Ad veri scripserit scribentur cum."
            + "  </p>"
            + "  <p>"
            + " Iudico sadipscing ei eum. Sit cu utinam tempor pertinacia. Ea quis agam mazim qui, his amet hinc natum cu. Nulla delectus ut cum, in sit novum putent, ut bonorum docendi maiestatis sed. Eu nusquam salutandi duo, quo dolores tincidunt cu. Has te congue labore graecis."
            + "  </p>"
            + "  <p>"
            + " Te sit magna utroque. Eu per urbanitas definitionem, per zril legere fabulas et. Cu quo eros audire iudicabit, qui utinam docendi id, at pri nisl pertinacia. Id quo idque accusamus theophrastus. Adipiscing incorrupte est ne, eam exerci iuvaret corrumpit in. Nullam pertinax ei has."
            + "  </p>"
            + "  <p>"
            + " Eum ei dico consectetuer, audiam appellantur eam at. Mel at veri oratio phaedrum, ei vel quot liber. Suas clita consequat usu at. Ut est facer putent, tempor aliquid ex est. Lorem everti bonorum pro et, usu ex fugit tacimates adipiscing, ius ne option moderatius. Vix ea magna mutat, ex mei deleniti liberavisse. Dolore explicari delicatissimi sed ei, ex vel ornatus phaedrum."
            + "  </p>"
            + "  <p>"
            + " Mea ne nulla scripta corrumpit. Cu alterum singulis mel. Et duo illud inimicus erroribus. Ne sed diam aeterno elaboraret."
            + "  </p>"
            + "  <p>"
            + " Inimicus tractatos ullamcorper mea ei, offendit convenire no mea. An decore efficiendi sed, mentitum voluptatibus sed cu. An constituto consectetuer pro. Eu sit graece oporteat, in sea oportere patrioque. Nam ei omnis conclusionemque, enim possim dolorum eu nec, at nonumes lucilius vel. Id vivendo intellegat pri, idque possim persius pro at, et vim harum voluptaria. Sonet viris torquatos an duo, sea suas congue volutpat id, in eum ubique necessitatibus."
            + "  </p>"
            + "  <p>"
            + " Vel deleniti mandamus reformidans ad, inciderint quaerendum vituperatoribus ei mel. Te his vidit blandit, ut mea persius comprehensam. Ex dico phaedrum pertinax eam. Ex vel invidunt volutpat efficiendi."
            + "  </p>"
            + "  <p>"
            + " Cu pri atqui posidonium, persequeris deterruisset cu his. Pri cu dicant latine, ne delectus deserunt tincidunt nec. Usu alii viris invidunt ne, tempor aliquid instructior ea mel. Nam viris assentior efficiendi ea, ei est aeterno gubergren. Debet error accumsan ad ius, vidit ubique timeam vel et."
            + "  </p>"
            + "  <p>"
            + " Nam te numquam recusabo, dicat semper signiferumque at mea. Vim diam tollit liberavisse no, sea fierent instructior eu. Eu nihil dolore efficiendi pri. Sit veri error ut, ad case hendrerit his."
            + "  </p>"
            + "  <p>"
            + " Cum fierent luptatum at, perfecto gubergren at usu. Cu mel nibh suscipit dignissim. Ex has tritani accusata, ius verear percipitur dissentias et. At eos veniam consulatu. Nulla homero vix in. Nec ne purto possit deleniti, minim eligendi sapientem cum at."
            + "  </p>"
            + "  <p>"
            + " Te aeterno fuisset concludaturque ius, usu te nisl appetere appellantur. Cu alterum philosophia instructior eum, usu soluta complectitur ne. Duo ei esse offendit. Novum homero duo ei, usu ex lucilius voluptatum, his altera quaeque platonem ex. Nec luptatum constituam appellantur ne, usu ea erant accusata. Possit vidisse docendi vel te, putent percipitur persequeris cum ea."
            + "  </p>"
            + "  <p>"
            + " Qui et sint mandamus. Te eirmod fierent abhorreant cum, augue adolescens nec et. An quo audiam convenire expetendis, eam no apeirian definitiones. Ei ubique ancillae expetenda eum, nonumy vocibus ancillae qui ea."
            + "  </p>"
            + "  <p>"
            + " Ea cum wisi quas senserit, qui choro dolor graeci ne, et nec ornatus percipitur. Eos postea mnesarchum reprimique ex, harum legendos mea no, per purto omnes denique eu. Labore offendit an mel, vel ad eruditi habemus temporibus. Pri vidit novum nostrum te, no eum movet ubique."
            + "  </p>"
            + "  <p>"
            + " In insolens quaerendum mediocritatem vel, dicit tempor probatus at est. Saperet suscipiantur id mei. Te prima putant sed, usu ad erat quaerendum. Eos id modo mucius propriae."
            + "  </p>"
            + "  <p>"
            + " In graece nusquam erroribus est. Consetetur disputationi usu et. Sit an prompta epicuri vituperatoribus, te labores gloriatur est. Cum habeo mediocritatem cu, tritani oportere ei has. Agam inani tamquam an est."
            + "  </p>"
            + "  <p>"
            + " Et sea diceret scripserit, in mel mucius quaeque singulis, essent facilis officiis eum ex. No malis bonorum consequuntur vix. Nullam offendit quaestio no has. Vim ad fabellas contentiones definitiones, has ei esse nibh constituam, vel natum labitur salutatus et. Vis solet indoctum postulant ex, nemore epicuri ancillae ut nam."
            + "  </p>"
            + "  <p>"
            + " Modo facete ea sed, ei putent dissentiunt cum, sed nihil altera an. Ei animal definiebas his. Dicant deserunt conclusionemque te pro, choro suavitate ex eos, partem appetere consetetur vix ad. Vidisse admodum temporibus et per, graece voluptua vulputate in per. Hinc suscipiantur vel ei, menandri moderatius eum ei."
            + "  </p>"
            + "  <p>"
            + " Ea cum rebum insolens, adhuc exerci invidunt mei at. Sit ei omnis mucius vituperata, est eu brute reque. Qui ex nisl option adipisci. Pri vide dicunt nominavi at, vitae lobortis signiferumque cu est. Errem scriptorem ei cum, iusto delicata pro eu."
            + "  </p>"
            + "  <p>"
            + " Sea quod invenire eu, no nec facete oblique molestiae, eu mei phaedrum tractatos. Ei alienum perpetua temporibus duo. Movet noluisse recteque eos at. Quis consectetuer cu eam, placerat reformidans eu his. Ad pri ornatus platonem iudicabit, pro ne iusto feugiat. Omnium forensibus temporibus ad nec."
            + "  </p>"
            + "  <p>"
            + " Et usu habemus recusabo adipiscing, enim molestie argumentum eum et. Ad vel ubique alterum nominavi, an prima dictas deleniti eum. Augue saepe efficiantur vis ne. Est modo ferri eruditi ne. Quo ad dicta ubique, usu justo soluta debitis an, dico tritani vim te. No zril accumsan mnesarchum pro, cu illum periculis eos. Sed inani semper eu, sint theophrastus contentiones ne vim, facer doctus interpretaris vis ne."
            + "  </p>"
            + "  <p>"
            + " Sea nulla nonumes percipit eu. Timeam legimus perpetua ex mei, eu velit fierent vim. Vim altera moderatius necessitatibus an, eos ei quod choro iudico, duo possim fuisset no. An mei rebum atqui gubergren, no possim qualisque usu, debet imperdiet vix cu."
            + "  </p>"
            + "  <p>"
            + " Saepe tollit audiam ut his, iisque commodo eos an. Affert numquam delicatissimi ea duo, mel eu rebum appetere liberavisse, duo lorem patrioque similique et. Vim viris graecis ex. Ei hinc nullam noluisse sea, reque causae ei eum. Dolorum explicari no vel, congue lucilius efficiantur te eam. Vide probo oratio no vix. Ad pertinax patrioque eos."
            + "  </p>"
            + "  <p>"
            + " Vim reque partem legere an, ad nam mazim legendos. Cetero denique adipiscing ea vel, usu in elitr partiendo, cu erant consequat duo. Ne summo inermis has, mea graeco persius et, et mel tation ponderum. Per te iudico eloquentiam accommodare. Eu doctus persecuti nam. Nibh dicunt ad has."
            + "  </p>"
            + "  <p>"
            + " Graece graecis omnesque eum ad, volutpat aliquando deterruisset ad eum, eam ea paulo cetero labores. Vel ex aperiam salutandi, usu hendrerit signiferumque ad. Porro argumentum per ad. Eu ancillae rationibus contentiones eos. Ea ius eius velit deserunt, vivendum scriptorem eum ut."
            + "  </p>"
            + "  <p>"
            + " Solum inani dicam in mel, forensibus elaboraret instructior has te, ei eripuit accusam molestie vel. His et saperet democritum reprehendunt. An vel odio consetetur, sed ad tacimates omittantur. Te mea consul ocurreret."
            + "  </p>"
            + "  <p>"
            + " Usu quem causae insolens ad. Nam rebum vocibus explicari id. Mea ubique delectus deterruisset in, errem altera tritani sed ut, nec et aeterno discere. Ad duo zril ornatus temporibus, id soleat percipitur sit. Te civibus repudiandae interpretaris cum, quo et vivendo posidonium, vim ex case moderatius."
            + "  </p>"
            + "  <p>"
            + " His ex noster maiestatis, dicat latine tacimates ex vis. Alia nemore evertitur at usu, eu duo quis feugait accusata. Minim mediocrem ut sea. Est in modo volumus. In usu timeam vivendo intellegat, denique referrentur ea eam, in fierent persequeris usu."
            + "  </p>"
            + "  <p>"
            + " Ex vim wisi congue numquam, pro nostrum elaboraret cu. Cum ut labitur evertitur, falli impetus tritani pri ex. Id essent invenire eleifend his, eam nominavi consectetuer id. Ut antiopam molestiae mel, mea ferri assum timeam ut. Equidem forensibus appellantur has an, homero explicari qui cu, quo option utamur inermis an."
            + "  </p>"
            + "  <p>"
            + " Nec eu affert mollis liberavisse. Ne mei assum sadipscing. Tamquam mediocritatem mel no, ad per ubique postea labores. Usu id hinc vocibus."
            + "  </p>"
            + "  <p>"
            + " Ut mei veniam persius sensibus, vim ea sumo postulant. Errem graece platonem sea ad. Qui ut audiam vivendum, mea purto disputando eu. Utinam persequeris sea in."
            + "  </p>"
            + "  <p>"
            + " Ad per odio case minimum. An probo percipit eum, sea cu illum gubergren. Ei sea unum aeterno denique, ei quidam neglegentur sea. Ad vocibus omittantur nam, ea oblique principes usu, case insolens constituto vix ad. Mei at illum reprimique, sit sumo constituto te."
            + "  </p>"
            + "  <p>"
            + " Vel tritani efficiantur et, mei illud facilisi partiendo ad. Cu feugait erroribus vel. Ei mea nusquam copiosae accommodare. Sea eu quis aliquip, integre vivendum patrioque vis in, at facer labitur inimicus quo. Magna periculis voluptatum ei mea."
            + "  </p>"
            + "  <p>"
            + " Ius ut consulatu dissentias, ceteros vituperatoribus nec id. Has cu veritus noluisse facilisi, pri eu augue concludaturque. No tollit tincidunt usu. Errem dissentiet eum ea. Quod sumo falli ei vel. Duo cu verterem omittantur. In liber oblique vivendum sit."
            + "  </p>"
            + "  <p>"
            + " Eum ea case nulla congue, vel tale noluisse ut, augue nostro neglegentur id vel. Id tantas inermis efficiantur per, mollis euismod percipit eos ea, id dolore soluta eleifend pri. Eum elit natum no. Ut accusam efficiantur sit, cu eos iuvaret splendide accommodare. Usu suscipit appellantur et, corpora dissentias pri cu."
            + "  </p>"
            + "  <p>"
            + " No pro liber option, vitae luptatum vis ne. Et latine qualisque delicatissimi mei. Eum ne autem vitae, an nostro contentiones sea, sit impedit interesset ei. Quot tibique appetere eu eum, nam dicta utamur posidonium id. Oporteat urbanitas id nam, graeco sensibus delicatissimi id vim. Has dolore sententiae voluptatibus te, at vim utamur minimum efficiantur."
            + "  </p>"
            + "  <p>"
            + " Eam et eius detracto apeirian, saepe interesset ad ius, id vix insolens omittantur. Ei vim noster ponderum necessitatibus, mel intellegat interpretaris an. Ex ponderum cotidieque vix, at nostrum nominati repudiare per, et mel labore facilisis. Legere maiestatis definiebas mel ea, ex oporteat posidonium eum."
            + "  </p>"
            + "  <p>"
            + " Eos labitur delectus no, modo partem ei qui. Sea summo erant ponderum ut, sed lobortis definitiones ad. In pri zril neglegentur, probo laboramus mea id, eam ea verterem antiopam imperdiet. Ut vix graece omnesque consectetuer. Id mea quaeque eligendi. Ne sit lucilius platonem, vis ne dicit honestatis."
            + "  </p>"
            + "  <p>"
            + " Vis maiorum denique accumsan in, quas denique quaerendum mea et. Sit ubique omittantur ut. Pri ea diam quodsi splendide, ea duo vidit commune. Usu nibh fabellas conceptam te. Quod falli his ea, sea sumo volumus in. At eam patrioque persecuti, usu cu etiam lobortis."
            + "  </p>"
            + "  <p>"
            + " In pri dicat corpora adipisci, has ullum elitr ad. Eu nostro facete albucius per, mea id exerci legendos sententiae. Sea ut eros sanctus civibus. Et ius laudem maiorum, ex esse eligendi singulis has."
            + "  </p>"
            + "  <p>"
            + " Usu utinam luptatum ne, an vel viderer impedit dissentiet. Ridens percipit sadipscing id pro, ne liber viris docendi mea. Ius regione persequeris contentiones ad. Ut sensibus senserit posidonium sea, duo an habeo saepe petentium."
            + "  </p>"
            + "  <p>"
            + " Tota nusquam quo ex, omnes semper voluptua ut per, mea ei possit feugiat delicata. Senserit convenire assentior ea sit, nec atqui quaerendum ad. Ad errem primis forensibus ius. Mei scripta eripuit id. Qui ea lorem partem animal."
            + "  </p>"
            + "  <p>"
            + " Ex putant integre mei, iuvaret inciderint ei duo. Agam posse facer at duo, prompta maiorum pri at, et sea sint delicata mediocrem. Id usu aeque incorrupte dissentiunt, eu epicurei scribentur liberavisse mei, detracto sententiae moderatius ex duo. Case dicunt audiam eu has. Erant lobortis cum ut, ne vero salutatus comprehensam qui. Eu alii accusamus mea, eu minim lobortis suscipiantur eum."
            + "  </p>"
            + "  <p>"
            + " Lobortis volutpat te mei, no magna feugait adversarium duo. Cum ut omittam elaboraret inciderint. Dicat malorum ea eam, eu fierent molestie lobortis quo. Duo at autem assum tation."
            + "  </p>"
            + "  <p>"
            + " Sea ea graece utroque fabellas. Duo modus dicant quodsi in, mea no nonumes definiebas, nam ea diceret omittantur. Usu ad mutat offendit, labitur alienum posidonium ut pri. Ius clita delectus maiestatis an, vis et solet quidam prodesset. Movet vocent pri cu. Duo eu delectus explicari cotidieque, ius in choro causae graecis, mel suscipit percipitur no."
            + "  </p>"
            + "  <p>"
            + " Ius eu sumo euripidis suscipiantur, vis vocent ponderum id, vel eu molestie invenire quaestio. Nusquam omittam ei sea, dolore indoctum vel ei, nam wisi nusquam dissentias an. Usu cu erant conceptam disputationi, sea ancillae percipitur an. Vitae congue te mea."
            + "  </p>"
            + "  <p>"
            + " His doming nominati suscipiantur id, qui postulant convenire an. Causae vocibus suscipit eos et. Tempor adversarium duo ad. Mei ut aeterno numquam, solet aliquip maiestatis nam et. Nominavi ocurreret conceptam ea vim, nisl mucius ea mea."
            + "  </p>"
            + "  <p>"
            + " Cum et graeci inermis elaboraret. Ne augue mentitum postulant vel. Percipit delicatissimi per ad, nec probo dictas id. Mundi patrioque euripidis ad sit, ut mei habeo error."
            + "  </p>"
            + "  <p>"
            + " No quem vide adolescens per, sit id choro eligendi, mel ex facer torquatos. Tation consequat ut per. Cu aeque equidem ius. Laoreet scripserit philosophia his ne, ut est gubergren mediocritatem. At sensibus consetetur est."
            + "  </p>"
            + "  <p>"
            + " Debet ponderum eu sea. Ius iusto accumsan albucius ut. Ut adipisci pertinax torquatos sea, nam ut vitae periculis delicatissimi. Vis natum audire molestiae ea, id praesent scriptorem nam. Pri ipsum ridens eripuit at, facer honestatis mel id, eam illum mentitum hendrerit cu. Ius et sint moderatius, sea ad euismod volutpat. Nam decore utamur ea, mutat nominavi an nam."
            + "  </p>"
            + "  <p>"
            + " At numquam suscipiantur eos, graecis argumentum te mei, usu ad ludus probatus. Cu pertinax partiendo duo. Ex ius summo doctus, delenit officiis te qui. Porro prompta ex has."
            + "  </p>"
            + "  <p>"
            + " Diam dicam labores ne sed, errem partem accusata eu duo, ius possit dolores fabellas in. In scriptorem disputando per, quidam ceteros evertitur mea an. Ridens elaboraret an eam. Omnes nostrud cu per. Nam postea bonorum ut."
            + "  </p>"
            + "  <p>"
            + " In nec simul vivendum. Quaeque officiis tacimates mel eu, est eu assum insolens, sed eius philosophia ex. Definitiones mediocritatem ex usu. Nec no dicunt veritus dissentias, te vix equidem iracundia. Id nec aliquid persequeris, dolor scriptorem ea eos."
            + "  </p>"
            + "  <p>"
            + " Mel ex simul accusamus expetendis. Vix eros nostro petentium an. Quem inermis recusabo et nec, nec te debitis mediocritatem, ea cum tale evertitur. Mei assum populo detracto ne, in est moderatius comprehensam, eu qui agam utinam doming. Cum nusquam docendi ad, odio accommodare eam ne, quo ea quod probatus explicari."
            + "  </p>"
            + "  <p>"
            + " Eum no docendi vulputate adolescens, est ne sonet dictas volutpat. Exerci sapientem quo cu, omnium suavitate ut nam. Ad pro doctus sanctus comprehensam. Ut amet assueverit scribentur duo, impetus labores constituam an pri, ex consul placerat platonem eam."
            + "  </p>"
            + "  <p>"
            + " Modus voluptua sententiae an vix, eum malorum persius te, placerat abhorreant disputationi vix ex. Ut zril patrioque posidonium cum, viderer detracto omnesque at usu. Audiam dolores est id, pri no deleniti salutatus. No tollit aperiri ullamcorper sea, est et stet scaevola. Mel graeci adipisci appellantur ex, in mutat tritani liberavisse has."
            + "  </p>"
            + "  <p>"
            + " Nec ei veri recteque. Usu augue labitur comprehensam ex, eu vivendo detraxit cum. Et nonumy forensibus has, pri ei audire ceteros, sonet delectus mei ei. An quo veritus suscipit, mea ne probo dictas similique."
            + "  </p>"
            + "  <p>"
            + " Mei ne putent nominavi accusamus, an eos munere forensibus deterruisset, qui dico consetetur ei. No vel audire eripuit veritus, ea vim feugiat nominavi referrentur. In dolore recteque dissentiunt usu, ius ludus suscipit ei. Augue iudico scripserit nec eu, sea maiorum mentitum signiferumque at, honestatis accommodare ei pri. Clita expetenda elaboraret ius te, ea copiosae theophrastus vituperatoribus mea, usu qualisque adversarium ut. At congue oratio quo."
            + "  </p>"
            + "  <p>"
            + " Suavitate voluptatum vim eu. Sed eu error intellegam, an quidam fierent vix. Ei quo nemore tincidunt intellegam, nec at consul soleat numquam. Et pri meis lucilius gubergren. Mel cu altera tractatos conceptam. Eam alia saepe dicunt ex, ea has wisi dicit dissentias."
            + "  </p>"
            + "  <p>"
            + " Amet placerat perfecto mei cu. Porro possim pro ne, ad graeco torquatos philosophia eam. Reformidans consectetuer no mei. An wisi ornatus assentior eos, an nec facete accumsan. Vis id doctus impetus quaerendum, his unum signiferumque eu."
            + "  </p>"
            + "  <p>"
            + " Timeam lobortis mei at. Tota latine theophrastus ad quo, dolore ridens democritum ea cum. Commune percipitur cu est, eos at quidam placerat, est te mazim essent periculis. Ferri tantas vel at, odio menandri consequat pro ea. Mel ex quidam graecis constituam, vero vituperata assueverit pro cu. Qui sumo aeterno et, apeirian mandamus pro no."
            + "  </p>"
            + "  <p>"
            + " Eleifend referrentur te nec, tempor probatus per cu. No vim purto molestie tincidunt. Etiam admodum constituto his et. Elitr percipitur nam te, eu nam partem dictas. Sit munere suscipit ad. Id posse percipit vim."
            + "  </p>"
            + "  <p>"
            + " Vis ex euismod accumsan insolens. Ius ferri aliquid te, id sed amet meis laudem, qui splendide tincidunt adolescens eu. Semper habemus neglegentur te quo. Diam argumentum at qui, ut sed meis disputationi. Ne porro velit sed, ut mea liber corpora detracto, id sed alterum sanctus. Prima deterruisset te sea. Nam fabulas antiopam cu."
            + "  </p>"
            + "  <p>"
            + " Latine offendit eum id. Modus animal tincidunt at ius. In vim verear dolores commune. Odio facer his ei. Vel insolens appellantur ad, mea at veri persius maiorum. Nec mutat simul appetere ne, ex nam tale erant eleifend."
            + "  </p>"
            + "  <p>"
            + " Luptatum probatus inimicus vel ad, minim possit interesset eum te. Cum dicam latine utroque an, id quot facilis dolores his. Per ad appetere erroribus adolescens, amet vitae intellegebat ea eam, at nam nonumy mnesarchum philosophia. Ad putent splendide interesset mel, no adipisci mnesarchum pro, in per temporibus persequeris. An assum aliquid usu, sit eu alia elitr, audire scripserit qui ea. Noster verear facilisis usu ex, ei sit euismod accumsan, te causae facilisi mel."
            + "  </p>"
            + "  <p>"
            + " At duis quodsi debitis pri, duo omittam iracundia signiferumque ei. Nam ne decore cetero pertinacia. Ex sea reque ipsum. Primis aliquando vel ea."
            + "  </p>"
            + "  <p>"
            + " Omnis deterruisset at qui. Putant offendit his at. Cum at diam essent, per eu postea repudiandae. Cu quo vocibus principes disputationi, omnes voluptaria eos ex."
            + "  </p>"
            + "  <p>"
            + " Ne stet inermis sapientem per, ex vix quem complectitur, id nam ipsum putent antiopam. Te sed vocent veritus, mei adolescens repudiandae delicatissimi ut. Viris ponderum rationibus in sea. Cum quod quidam eu, ius illum homero ea, ne virtute elaboraret mel. Soleat habemus definitionem ius eu, solum libris animal eos te, pri alia stet integre at."
            + "  </p>"
            + "  <p>"
            + " Reque perpetua cu vis, nam ad possim accusam principes. An vis vocent iracundia, velit bonorum vel in. Cu alii vulputate cotidieque nam. Corpora comprehensam at est. Cum ut noster eirmod deterruisset. Fabulas utroque appellantur id vix, decore torquatos intellegat qui an. Graeco impedit duo in."
            + "  </p>"
            + "  <p>"
            + " Dicit ridens et vis. Quo in veri lucilius, his quis democritum ut. Ea per dolor conceptam omittantur. Unum aliquam sea te, sea saepe detraxit et, cum et possim doctus."
            + "  </p>"
            + "  <p>"
            + " Ut eos ullum facer, sit id solum sonet partem. Cum an oratio dolores aliquando, ut dico civibus torquatos pri, quas appetere recteque an per. Quod maiestatis eam te. Ridens possim pri ad, esse salutandi dissentiunt has cu, mei eu aperiam feugiat fabellas."
            + "  </p>"
            + "  <p>"
            + " Error harum ex eos. Malis delectus vituperatoribus ad nam, an eos paulo antiopam, vocent inimicus at vis. Pri summo iriure ut, ex mel eripuit utroque assueverit. Nec autem elitr scripserit eu. Ei qui possit dolores reprimique, pro discere diceret ea."
            + "  </p>"
            + "  <p>"
            + " Sale vivendum eum ad. Cu probo magna cum. Nec omnes tation eu, facer nonumes cu vim. Et porro fierent partiendo eam, vis te posse scribentur, duo aeque prompta bonorum eu."
            + "  </p>"
            + "  <p>"
            + " An mei petentium scriptorem, ei ius purto veniam. Ius legimus ullamcorper ea. Est ut blandit molestie facilisi. Vis te ullum maiorum denique, te sed probatus interesset suscipiantur, his debet equidem cotidieque cu. Eam id aperiri integre intellegebat, cu duo accumsan expetendis concludaturque, id patrioque voluptatum complectitur has. Ex laboramus consetetur vis, pri persius apeirian ex."
            + "  </p>"
            + "  <p>"
            + " Id nam probo mandamus, in probo."
            + "</p>";

    static {
        Arrays.sort(attCodes);
    }

    /** Vytvoří testovací data. Databáze nemusí být prázdná. */
    @Transactional
    @RequestMapping(value = "/createData", method = RequestMethod.POST)
    public void createData() {
        RulArrangementType arrArrangementType = arrangementTypeRepository.findAll().iterator().next();
        RulRuleSet ruleSet = ruleSetRepository.findAll().iterator().next();
        ArrFindingAid findingAid = arrangementManager.createFindingAid(FA_NAME, arrArrangementType.getArrangementTypeId(), ruleSet.getRuleSetId());
        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());

        List<RegRecord> records = createRegRecords();
        List<ParParty> parties = createParties(records);
        createTree(MAX_DEPTH, NODES_IN_LEVEL, version, parties);
    }

    private List<ParParty> createParties(List<RegRecord> records) {
        Map<String, ParPartySubtype> partySubTypeMap = partySubtypeRepository.findAll().stream()
            .collect(Collectors.toMap(ParPartySubtype::getCode, Function.identity()));

        List<ParParty> existingParties = partyRepository.findAll();

        List<ParParty> parties = new LinkedList<ParParty>();
        for (PartyEnum partyEnum : PartyEnum.values()) {
            String subType = partyEnum.getSubType();
            int position = partyEnum.getRecordPosition();
            ParPartySubtype partySubtype = partySubTypeMap.get(subType);
            RegRecord regRecord = records.get(position);
            ParParty party = findParty(partySubtype, regRecord, existingParties);
            if (party == null) {
                party = createParParty(partySubtype, regRecord);
            }
            parties.add(party);
        }

        return parties;
    }

    private enum PartyEnum {

        KOR_SPOL("KOR_SPOL", 0),
        KOR_VEREJNE("KOR_VEREJNE", 1),
        ROD("ROD", 2),
        VETEV_ROD("VETEV_ROD", 3),
        FYZ_OSOBA1("FYZ_OSOBA", 4),
        FYZ_OSOBA2("FYZ_OSOBA", 5);

        private String subType;
        private int recordPosition;

        private PartyEnum(String subType, int recordPosition) {
            this.subType = subType;
            this.recordPosition = recordPosition;
        }

        public String getSubType() {
            return subType;
        }

        public int getRecordPosition() {
            return recordPosition;
        }
    }

    private ParParty findParty(ParPartySubtype parPartySubtype, RegRecord regRecord,
            List<ParParty> existingParties) {
        if (existingParties.isEmpty()) {
            return null;
        }

        for (ParParty party : existingParties) {
            if (party.getPartySubtype().equals(parPartySubtype)
                    && party.getRecord().equals(regRecord)) {
                return party;
            }
        }

        return null;
    }

    private ParParty createParParty(ParPartySubtype parPartySubtype, RegRecord regRecord) {
        ParParty parParty = new ParParty();
        parParty.setPartySubtype(parPartySubtype);
        parParty.setRecord(regRecord);

        return partyRepository.save(parParty);
    }

    private List<RegRecord> createRegRecords() {
        Map<String, RegRegisterType> regTypesMap = registerTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RegRegisterType::getCode, Function.identity()));
        List<RegRecord> existingRegRecords = recordRepository.findAll();

        List<RegRecord> records = new LinkedList<RegRecord>();

        for (RegRecords regRecordEnum : RegRecords.values()) {
            RegRecord regRecord = findRegRecord(regRecordEnum, existingRegRecords, regTypesMap);
            if (regRecord == null) {
                regRecord = createRegRecord(regRecordEnum.getRecord(), regRecordEnum.getCharacteristics(), regRecordEnum.getRegistryType(), regTypesMap);
            }

            if (regRecordEnum.isReturnRecord()) {
                records.add(regRecord);
            }
        }

        return records;
    }

    /**
     * Zjistí jestli exituje rejstřík.
     *
     * @param regRecordEnum rejstřík
     * @param existingRegRecords exitující rejstříky
     * @param regTypesMap mapa typů rejstříků
     *
     * @return rejstřík pokud existuje, null když neexistuje
     */
    private RegRecord findRegRecord(RegRecords regRecordEnum, List<RegRecord> existingRegRecords, Map<String, RegRegisterType> regTypesMap) {
        if (existingRegRecords.isEmpty()) {
            return null;
        }

        for (RegRecord regRecord : existingRegRecords) {
            if (regRecord.getRecord().equals(regRecordEnum.getRecord())
                    && regRecord.getCharacteristics().equals(regRecordEnum.getCharacteristics())
                    && regRecord.getRegisterType().equals(regTypesMap.get(regRecordEnum.getRegistryType()))) {
                return regRecord;
            }
        }
        return null;
    }

    private enum RegRecords {

        TOPOL("Sbor dobrovolných hasičů Topol", "1886-, Topol (Chrudim, Česko), dobrovolný hasičský sbor", "PARTY_GROUP", true),
        CHRUDIM("Okresní úřad Chrudim", "(1990-2002), 1990-2002, Chrudim (Česko), úřad státní okresní správy, V/PFJ: OkÚ Chrudim (1990-2002); Okresní úřad Chrudim II (1990-2002); OkÚ Chrudim II (1990-2002)", "PARTY_GROUP", true),
        HABSBURKOVE("Habsburkové", "11. století-, evropský panovnický rod", "PERSON", true),
        SCHWARZENBERGOVE("Schwarzenbergové - orlická větev", "(větev rodu), 1802-1979, větev šlechtického rodu s rodovým sídlem v Čechách, V/PFJ: Orlická", "PERSON", true),
        NOVAK("MUDR. Novák Josef (1900-1980)", "1.1.1900 Kamenice (Jihlava, Česko) -+1.1.1980 Kamenice, (Praha-východ, Česko), praktický lékař v Kamenici u Prahy", "PERSON", true),
        JOHN("John Jaromír prof. (1882-1952),", "\"*16.4.1882 Klatovy (Česko) - +24.4.1952 Jaroměř (Náchod, Česko), spisovatel, novinář, středoškolský a vysokoškolský učitel, výtvarný estetik a kritik, překladatel z němčiny\"", "PERSON", true),
        LHOTICE("Nové Lhotice (Chrudim, Česko)", "katastrální území a místní část obce Liboměřice, V/PFJ: Niemeczke Lhoticze (Chrudim, Česko); Německé Lhotice (Chrudim, Česko); Lhotice (Chrudim, Česko", "GEO", false),
        KYRILL("Kyrill (2007 : bouře),", "\"5.1.2007-19.1.2007, tlaková níže vzniklá nad Atlantským oceánem, která se rozvinula , o ničivé bouře se sílou orkánu a zasáhla Evropu V/PFJ: orkán Kyrill; storm Kyrill\"", "EVENT", false),
        STADION("Letní stadion čp. 831/IV (Chrudim, ulice V Průhonech),", "\"1951-, Chrudim (Česko), všesportovní stadion V/PFJ: Stadion Emila Zátopka Chrudim (Chrudim, ulice V Průhonech), Letní stadion Chrudim (Chrudim, ulice V Průhonech); Stadion MFK Chrudim (Chrudim, ulice V Průhonech); Stadion AFK Chrudim (Chrudim, ulice V Průhonech); Letní stadion AFK Chrudim (Chrudim, ulice V Průhonech); Atletický stadion Chrudim (Chrudim, ulice V Průhonech); Transporťácký stadion Chrudim (Chrudim, ulice V Průhonech); Stadion Transporty Chrudim (Chrudim, ulice V Průhonech)\"", "ARTWORK", false),
        MALIRI("malíři", "MDT 75.071.1", "TERM", false);

        private String record;
        private String characteristics;
        private String registryType;
        private boolean returnRecord;

        private RegRecords(String record, String characteristics, String registryType, boolean returnRecord) {
            this.record = record;
            this.characteristics = characteristics;
            this.registryType = registryType;
            this.returnRecord = returnRecord;
        }

        public String getRecord() {
            return record;
        }

        public String getCharacteristics() {
            return characteristics;
        }

        public String getRegistryType() {
            return registryType;
        }

        public boolean isReturnRecord() {
            return returnRecord;
        }
    }

    private RegRecord createRegRecord(String record, String characteristics, String registryType,
            Map<String, RegRegisterType> regTypesMap) {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(record);
        regRecord.setCharacteristics(characteristics);
        regRecord.setRegisterType(regTypesMap.get(registryType));
        regRecord.setLocal(false);

        return recordRepository.save(regRecord);
    }

    private void createTree(int maxDepth, int nodesInLevel, ArrFaVersion version, List<ParParty> parties) {
        Queue<ArrNode> parents = new LinkedList<>();
        parents.add(version.getRootFaLevel().getNode());

        ArrFaChange change = new ArrFaChange();
        change.setChangeDate(LocalDateTime.now());
        changeRepository.save(change);
        Session session = entityManager.unwrap(Session.class);

        int depth = 0;
        while (depth < maxDepth) {
            Queue<ArrNode> newParents = new LinkedList<>();
            while (!parents.isEmpty()) {
                ArrNode parent = parents.poll();
                for (int position = 1; position <= nodesInLevel; position++) {
                    ArrFaLevel level = createLevel(change, parent, position);
                    ArrNode node = level.getNode();
                    createLevelAttributes(node, version, depth, parties);
                    newParents.add(node);
                };
                session.flush();
                session.clear();
            }
            parents = newParents;
            depth++;
        }
    }

    private void createLevelAttributes(ArrNode node, ArrFaVersion version, int depth, List<ParParty> parties) {
        ArrDescItemSavePack descItemSavePack = new ArrDescItemSavePack();
        descItemSavePack.setCreateNewVersion(true);
        descItemSavePack.setFaVersionId(version.getFaVersionId());
        descItemSavePack.setNode(node);

        List<ArrDescItemExt> descItems = new LinkedList<ArrDescItemExt>();
        descItemSavePack.setDescItems(descItems);
        descItemSavePack.setDeleteDescItems(new LinkedList<ArrDescItemExt>());

        List<RulDescItemTypeExt> descriptionItemTypes = ruleManager.getDescriptionItemTypes(version.getRuleSet().getRuleSetId());
        for (RulDescItemTypeExt rulDescItemTypeExt : descriptionItemTypes) {
            if (Arrays.binarySearch(attCodes, rulDescItemTypeExt.getCode()) >= 0) {
                String dtCode = rulDescItemTypeExt.getDataType().getCode();
                switch (dtCode) {
                    case DT_COORDINATES:
                        descItems.add(createCoordinatesValue(node, rulDescItemTypeExt));
                        if (repeat(rulDescItemTypeExt)) {
                            descItems.add(createCoordinatesValue(node, rulDescItemTypeExt));
                        }
                        break;
                    case DT_FORMATTED_TEXT:
                        descItems.add(createFormattedTextValue(node, rulDescItemTypeExt));
                        if (repeat(rulDescItemTypeExt)) {
                            descItems.add(createFormattedTextValue(node, rulDescItemTypeExt));
                        }
                        break;
                    case DT_INT:
                        descItems.add(createIntValue(node, rulDescItemTypeExt));
                        if (repeat(rulDescItemTypeExt)) {
                            descItems.add(createIntValue(node, rulDescItemTypeExt));
                        }
                        break;
                    case DT_PARTY_REF:
                        descItems.add(createPartyRefValue(node, rulDescItemTypeExt, parties));
                        if (repeat(rulDescItemTypeExt)) {
                            descItems.add(createPartyRefValue(node, rulDescItemTypeExt, parties));
                        }
                        break;
                    case DT_STRING:
                        descItems.add(createStringValue(node, rulDescItemTypeExt));
                        if (repeat(rulDescItemTypeExt)) {
                            descItems.add(createStringValue(node, rulDescItemTypeExt));
                        }
                        break;
                    case DT_TEXT:
                        descItems.add(createTextValue(node, rulDescItemTypeExt));
                        if (repeat(rulDescItemTypeExt)) {
                            descItems.add(createTextValue(node, rulDescItemTypeExt));
                        }
                        break;
                    case DT_UNITDATE:
                        descItems.add(createUnitDateValue(node, rulDescItemTypeExt));
                        if (repeat(rulDescItemTypeExt)) {
                            descItems.add(createUnitDateValue(node, rulDescItemTypeExt));
                        }
                        break;
                    case DT_UNITID:
                        descItems.add(createIntValue(node, rulDescItemTypeExt));
                        if (repeat(rulDescItemTypeExt)) {
                            descItems.add(createIntValue(node, rulDescItemTypeExt));
                        }
                        break;
                }
            } else if (LEVEL_TYPE_ATT_CODE.equals(rulDescItemTypeExt.getCode())) {
                descItems.add(createLevelTypeValue(node, rulDescItemTypeExt, depth));
            }
        }

        arrangementManager.saveDescriptionItems(descItemSavePack);
    }

    private boolean repeat(RulDescItemTypeExt rulDescItemTypeExt) {
        List<RulDescItemConstraint> constraintList = rulDescItemTypeExt.getRulDescItemConstraintList();
        if (constraintList == null || constraintList.isEmpty()) {
            return RandomUtils.nextBoolean();
        }

        boolean repeatable = false;
        for (RulDescItemConstraint constraint : constraintList) {
            if (constraint.getRepeatable()) {
                repeatable = true;
                break;
            }
        }

        return repeatable && RandomUtils.nextBoolean();
    }

    private ArrDescItemExt createLevelTypeValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt, int depth) {
        ArrDescItemExt descItemExt = createValueWithoutspecification(node, rulDescItemTypeExt);
        descItemExt.setData(COORDINATES_VALUE);

        //všude hodnota 1, kromě ZP2015_LEVEL_FOLDER (pro 2. úroveň = 1, pro 3. úroven = 2, ...)
        String value;
        switch (depth) {
            case 0: //první úroveň ZP2015_LEVEL_SERIES
                chooseAndSetSpecification(descItemExt, rulDescItemTypeExt, LEVEL_TYPE_SERIES);
                value = "1";
                break;
            case (MAX_DEPTH - 2): //předposlední úroveň ZP2015_LEVEL_ITEM
                chooseAndSetSpecification(descItemExt, rulDescItemTypeExt, LEVEL_TYPE_ITEM);
                value = "1";
                break;
            case (MAX_DEPTH - 1): //poslední úroveň ZP2015_LEVEL_PART
                chooseAndSetSpecification(descItemExt, rulDescItemTypeExt, LEVEL_TYPE_PART);
                value = "1";
                break;
            default: //druhá a další úroveň až po předpředposlední ZP2015_LEVEL_FOLDER
                chooseAndSetSpecification(descItemExt, rulDescItemTypeExt, LEVEL_TYPE_FOLDER);
                value = Integer.toString(depth);
        }
        descItemExt.setData(value);

        return descItemExt;
    }

    private void chooseAndSetSpecification(ArrDescItemExt descItemExt, RulDescItemTypeExt rulDescItemTypeExt,
            String specCode) {
        for (RulDescItemSpec spec : rulDescItemTypeExt.getRulDescItemSpecList()) {
            if (spec.getCode().equals(specCode)) {
                descItemExt.setDescItemSpec(spec);
            }
        }
    }

    private ArrDescItemExt createPartyRefValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt, List<ParParty> parties) {
        ArrDescItemExt descItemExt = createValue(node, rulDescItemTypeExt);

        ParParty parParty = parties.get(RandomUtils.nextInt(parties.size()));
        descItemExt.setParty(parParty);
//        descItemExt.setData(parParty.getRecord().getRecord());
//        descItemExt.setRecord(parParty.getRecord());

      return descItemExt;
    }

    private ArrDescItemExt createCoordinatesValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItemExt descItemExt = createValue(node, rulDescItemTypeExt);
        descItemExt.setData(COORDINATES_VALUE);

        return descItemExt;
    }

    private ArrDescItemExt createUnitDateValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItemExt descItemExt = createValue(node, rulDescItemTypeExt);
        descItemExt.setData(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        return descItemExt;
    }

    private ArrDescItemExt createIntValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItemExt descItemExt = createValue(node, rulDescItemTypeExt);
        descItemExt.setData(Integer.toString(RandomUtils.nextInt(MAX_INT_VALUE)));

        return descItemExt;
    }

    private ArrDescItemExt createValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItemExt descItemExt = createValueWithoutspecification(node, rulDescItemTypeExt);
        descItemExt.setDescItemSpec(chooseSpec(rulDescItemTypeExt.getRulDescItemSpecList()));

        return descItemExt;
    }

    private ArrDescItemExt createValueWithoutspecification(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItemExt descItemExt = new ArrDescItemExt();
        descItemExt.setNode(node);
        descItemExt.setDescItemType(rulDescItemTypeExt);

        return descItemExt;
    }

    private ArrDescItemExt createStringValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItemExt descItemExt = createValue(node, rulDescItemTypeExt);
        descItemExt.setData(Integer.toString(RandomUtils.nextInt(MAX_INT_VALUE)));

        return descItemExt;
    }

    private ArrDescItemExt createTextValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItemExt descItemExt = createValue(node, rulDescItemTypeExt);
        descItemExt.setData(TEXT_VALUE);

      return descItemExt;
    }

    private ArrDescItemExt createFormattedTextValue(ArrNode node, RulDescItemTypeExt rulDescItemTypeExt) {
        ArrDescItemExt descItemExt = createValue(node, rulDescItemTypeExt);
        descItemExt.setData(FORMATTED_TEXT_VALUE);

      return descItemExt;
    }

    private RulDescItemSpec chooseSpec(List<RulDescItemSpecExt> rulDescItemSpecList) {
        if (rulDescItemSpecList == null || rulDescItemSpecList.isEmpty()) {
            return null;
        }

        int size = rulDescItemSpecList.size();
        return rulDescItemSpecList.get(RandomUtils.nextInt(size));
    }

    private ArrFaLevel createLevel(final ArrFaChange createChange, final ArrNode parentNode, final Integer position) {
        Assert.notNull(createChange);

        ArrFaLevel level = new ArrFaLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setParentNode(parentNode);
        level.setNode(createNode());
        return levelRepository.save(level);
    }

    private ArrNode createNode() {
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        return nodeRepository.save(node);
    }

    /** Odstraní data z databáze, kromě tabulek s prefixem rul_. */
    @Transactional
    @RequestMapping(value = "/removeData", method = RequestMethod.DELETE)
    public void removeData() {
        versionRepository.deleteAllInBatch();
        findingAidRepository.deleteAllInBatch();
        dataRepository.deleteAllInBatch();
        descItemRepository.deleteAllInBatch();
        partyRepository.deleteAllInBatch();
        externalSourceRepository.deleteAllInBatch();
        variantRecordRepository.deleteAllInBatch();
        recordRepository.deleteAllInBatch();
        levelRepository.deleteAllInBatch();
        nodeRepository.deleteAllInBatch();
        changeRepository.deleteAllInBatch();
    }
}
