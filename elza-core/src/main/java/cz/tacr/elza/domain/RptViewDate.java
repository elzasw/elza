package cz.tacr.elza.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "rpt_view_date")
public class RptViewDate {

    @Id
    private LocalDate dateId;

    @Column(name = "date_year")
    private Integer year;

    @Column(name = "date_quarter")
    private Integer quarter;

    @Column(name = "date_month")
    private Integer month;

    @Column(name = "date_day")
    private Integer day;

    @Column(name = "date_day_of_week")
    private Integer dayOfWeek;

    private Boolean isHolyday;

	public LocalDate getDateId() {
		return dateId;
	}

	public void setDateId(LocalDate dateId) {
		this.dateId = dateId;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public Integer getQuarter() {
		return quarter;
	}

	public void setQuarter(Integer quarter) {
		this.quarter = quarter;
	}

	public Integer getMonth() {
		return month;
	}

	public void setMonth(Integer month) {
		this.month = month;
	}

	public Integer getDay() {
		return day;
	}

	public void setDay(Integer day) {
		this.day = day;
	}

	public Integer getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(Integer dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public Boolean getIsHolyday() {
		return isHolyday;
	}

	public void setIsHolyday(Boolean isHolyday) {
		this.isHolyday = isHolyday;
	}
}
