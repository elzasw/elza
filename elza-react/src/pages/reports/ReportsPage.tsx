import { Icon, Ribbon } from "components/index.jsx";
import PageLayout from "../shared/layout/PageLayout";
import { Button } from "@fluentui/react-components";
import { downloadFile } from "actions/global/download";
import { Api, basePath } from "api";
import { ReportsForm, ReportsTable } from "components/reports";
import { WaitingOverlay } from "components/shared/waiting-overlay";
import {
    ReportApiAxiosParamCreator,
    ReportReportData,
    ReportReportFormat,
    ReportReportParameters,
    RequestProcessState,
} from "elza-api";
import { useState } from "react";
import { FormattedMessage, defineMessages } from "react-intl";
import { useSelector } from "react-redux";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import { SubmitDefinition } from "components/reports/ReportsForm";

const messages = defineMessages({
    generationTime: {
        id: "admin_reports_generation_time",
        defaultMessage: "Čas vygenerování přehledu: {date} v {time}"
    },
    downloadCSV: {
        id: "admin_reports_download_csv",
        defaultMessage: "Stáhnout CSV"
    },
    noReportData: {
        id: "admin_reports_no_report_data",
        defaultMessage: "Žádná data",
    }
})

export function ReportsPage() {
    const splitter = useSelector(({ splitter }: AppState) => splitter);
    const [reportData, setReportData] = useState<ReportReportData>();
    const [isFetchingReport, setIsFetchingReport] = useState(false);
    const [_isReportFetched, setIsReportFetched] = useState(false);
    const [lastReportId, setLastReportId] = useState<number>();
    const [lastReportDefinition, setLastReportDefinition] = useState<SubmitDefinition>();
    const dispatch = useThunkDispatch();

    const buildRibbon = () => {
        return <Ribbon admin={true} />;
    };

    const handleSubmit = async (definition: SubmitDefinition, params: ReportReportParameters) => {
        const definitionCode = definition?.definition?.code;

        if (!definitionCode) {
            throw "no selected report definition";
        }

        const { data: reportId } = await Api.report.reportGenerateReport(definitionCode, { ...params });

        setReportData(undefined);
        setIsFetchingReport(true);
        setIsReportFetched(false);
        setLastReportDefinition(definition);
        const intervalId = setInterval(async () => {
            try {
                const { data: reportState } = await Api.report.reportGetReportStatus(reportId);
                if (reportState === RequestProcessState.Error) {
                    clearInterval(intervalId);
                    setIsFetchingReport(false);
                    setIsReportFetched(true);
                }
                if (reportState === RequestProcessState.Finished) {
                    clearInterval(intervalId);
                    const { data: reportResult } = await Api.report.reportGetReport(reportId, ReportReportFormat.Json);
                    setReportData(reportResult);
                    setIsFetchingReport(false);
                    setIsReportFetched(true);
                    setLastReportId(reportId);
                }
            } catch (error) {
                clearInterval(intervalId);
                setIsFetchingReport(false);
                setIsReportFetched(true);
            }
        }, 2000);
    };

    async function handleDownload() {
        const { url } = await ReportApiAxiosParamCreator().reportGetReport(lastReportId, ReportReportFormat.Csv);
        dispatch(downloadFile(`${basePath}${url}`));
    }

    const reportDate = reportData ? new Date(reportData.sourceDataDate) : new Date();

    const centerPanel = (
        <div className="splitter-home">
            <div
                className="stats-container"
                style={{ display: "flex", flexDirection: "column", height: "100%", overflow: "hidden" }}
            >
                <div style={{ display: "flex", flexGrow: 1, flexShrink: 1, height: "100%" }}>
                    <div style={{ flexShrink: 0, borderRight: "var(--primary-border)" }}>
                        <ReportsForm onSubmit={handleSubmit} />
                    </div>
                    <div
                        style={{ flexGrow: 1, display: "flex", flexDirection: "column", overflow: "hidden", position: "relative" }}
                    >
                        {isFetchingReport && <WaitingOverlay />}
                        {reportData && (
                            <div style={{ flexShrink: 0, flexGrow: 0, display: "flex" }}>
                                <div style={{ padding: "8px", display: "flex", alignItems: "center" }}>
                                    <FormattedMessage id={`admin_reports_form_category_${lastReportDefinition.category.code}`} defaultMessage={lastReportDefinition.category.name} />
                                    &nbsp;<Icon glyph="fa-angle-right" />&nbsp;
                                    <b><FormattedMessage id={`admin_reports_form_report_${lastReportDefinition.definition.code}`} defaultMessage={lastReportDefinition.definition.name} /></b>
                                </div>
                                <div style={{ flex: 1 }} />
                                <div style={{ padding: "8px", display: "flex", alignItems: "center" }}>
                                    <div style={{ margin: "4px 16px" }}>
                                        <FormattedMessage
                                            {...messages.generationTime}
                                            values={{
                                                date: reportDate.toLocaleDateString(),
                                                time: reportDate.toLocaleTimeString(),
                                            }}
                                        />
                                    </div>
                                    <Button onClick={handleDownload}>
                                        <Icon glyph="fa-download" />
                                        &nbsp;
                                        <FormattedMessage {...messages.downloadCSV} />
                                    </Button>
                                </div>
                            </div>
                        )}
                        <div style={{ flexGrow: 1, display: "flex", flexDirection: "column", height: "300px" }}>
                            {!reportData ? (
                                <div style={{ padding: "16px", background: "var(--shade-2)", flex: 1 }}>
                                    {!isFetchingReport && <FormattedMessage {...messages.noReportData} />}
                                </div>
                            ) : (
                                <ReportsTable reportData={reportData} />
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );

    return <PageLayout splitter={splitter} ribbon={buildRibbon()} centerPanel={centerPanel} />;
}

export default ReportsPage;
