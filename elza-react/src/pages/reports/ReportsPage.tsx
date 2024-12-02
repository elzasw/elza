import { Icon, Ribbon } from "components/index.jsx";
import PageLayout from "../shared/layout/PageLayout";
// import { StatsHome } from "components/shared/stats";
import { useSelector } from "react-redux";
import { AppState } from "typings/store";
import { useState } from "react";
import { Api } from "api";
import {
    ReportReportData,
    ReportReportFormat,
    ReportReportParameters,
    ReportReportRow,
    ReportValue,
    ReportValueType,
    RequestProcessState,
} from "elza-api";
import { Button } from "@fluentui/react-components";
import { ReportsForm, ReportsTable } from "components/reports";
import { WaitingOverlay } from "components/shared/waiting-overlay";

function makeid(length) {
    let result = "";
    const characters = " ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    const charactersLength = characters.length;
    let counter = 0;
    while (counter < length) {
        result += characters.charAt(Math.floor(Math.random() * charactersLength));
        counter += 1;
    }
    return result;
}

export function ReportsPage() {
    const splitter = useSelector(({ splitter }: AppState) => splitter);
    const [reportData, setReportData] = useState<ReportReportData>();
    const [isFetchingReport, setIsFetchingReport] = useState(false);
    const [isReportFetched, setIsReportFetched] = useState(false);
    const [lastReportId, setLastReportId] = useState<number>();

    const buildRibbon = () => {
        return <Ribbon admin={true} />;
    };

    const handleSubmit = async (definitionCode: string, params: ReportReportParameters) => {
        if (!definitionCode) {
            throw "no selected report definition";
        }

        const { data: reportId } = await Api.report.reportGenerateReport(definitionCode, { ...params });

        setIsFetchingReport(true);
        setIsReportFetched(false);
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
        const { data: reportResult } = await Api.report.reportGetReport(lastReportId, ReportReportFormat.Csv);
        console.log("#### report download csv", reportResult);
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
                            <div style={{ flexShrink: 0, flexGrow: 0, padding: "8px", display: "flex", justifyContent: "flex-end" }}>
                                <div style={{ margin: "4px 16px" }}>
                                    Cas vygenerovani prehledu: {reportDate.toLocaleDateString()} v {reportDate.toLocaleTimeString()}
                                </div>
                                <Button onClick={handleDownload}>
                                    <Icon glyph="fa-download" />
                                    &nbsp;Stahnout CSV
                                </Button>
                            </div>
                        )}
                        <div style={{ flexGrow: 1, display: "flex", flexDirection: "column", height: "300px" }}>
                            {!reportData ? (
                                <div style={{ padding: "16px", background: "var(--shade-2)", flex: 1 }}>No report data</div>
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
