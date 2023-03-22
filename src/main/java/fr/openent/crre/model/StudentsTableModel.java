package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class StudentsTableModel implements IModel<StudentsTableModel>, Cloneable {
    private String idStructure;
    private Integer seconde;
    private Integer premiere;
    private Integer terminale;
    private Integer secondepro;
    private Integer premierepro;
    private Integer terminalepro;
    private Integer cap1;
    private Integer cap2;
    private Integer cap3;
    private Integer bma1;
    private Integer bma2;
    private Boolean pro;
    private Boolean general;
    private Integer totalApril;
    private Integer secondetechno;
    private Integer premieretechno;
    private Integer terminaletechno;

    public StudentsTableModel() {
    }

    public StudentsTableModel(JsonObject jsonObject) {
        this.idStructure = jsonObject.getString(Field.ID_STRUCTURE);
        this.seconde = jsonObject.getInteger(Field.SECONDE);
        this.premiere = jsonObject.getInteger(Field.PREMIERE);
        this.terminale = jsonObject.getInteger(Field.TERMINALE);
        this.secondepro = jsonObject.getInteger(Field.SECONDEPRO);
        this.premierepro = jsonObject.getInteger(Field.PREMIEREPRO);
        this.terminalepro = jsonObject.getInteger(Field.TERMINALEPRO);
        this.cap1 = jsonObject.getInteger(Field.CAP1);
        this.cap2 = jsonObject.getInteger(Field.CAP2);
        this.cap3 = jsonObject.getInteger(Field.CAP3);
        this.bma1 = jsonObject.getInteger(Field.BMA1);
        this.bma2 = jsonObject.getInteger(Field.BMA2);
        this.pro = jsonObject.getBoolean(Field.PRO);
        this.general = jsonObject.getBoolean(Field.GENERAL);
        this.totalApril = jsonObject.getInteger(Field.TOTAL_APRIL);
        this.secondetechno = jsonObject.getInteger(Field.SECONDETECHNO);
        this.premieretechno = jsonObject.getInteger(Field.PREMIERETECHNO);
        this.terminaletechno = jsonObject.getInteger(Field.TERMINALETECHNO);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    @Override
    public StudentsTableModel clone() {
        try {
            return (StudentsTableModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String getIdStructure() {
        return idStructure;
    }

    public StudentsTableModel setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public Integer getSeconde() {
        return seconde;
    }

    public StudentsTableModel setSeconde(Integer seconde) {
        this.seconde = seconde;
        return this;
    }

    public Integer getPremiere() {
        return premiere;
    }

    public StudentsTableModel setPremiere(Integer premiere) {
        this.premiere = premiere;
        return this;
    }

    public Integer getTerminale() {
        return terminale;
    }

    public StudentsTableModel setTerminale(Integer terminale) {
        this.terminale = terminale;
        return this;
    }

    public Integer getSecondepro() {
        return secondepro;
    }

    public StudentsTableModel setSecondepro(Integer secondepro) {
        this.secondepro = secondepro;
        return this;
    }

    public Integer getPremierepro() {
        return premierepro;
    }

    public StudentsTableModel setPremierepro(Integer premierepro) {
        this.premierepro = premierepro;
        return this;
    }

    public Integer getTerminalepro() {
        return terminalepro;
    }

    public StudentsTableModel setTerminalepro(Integer terminalepro) {
        this.terminalepro = terminalepro;
        return this;
    }

    public Integer getCap1() {
        return cap1;
    }

    public StudentsTableModel setCap1(Integer cap1) {
        this.cap1 = cap1;
        return this;
    }

    public Integer getCap2() {
        return cap2;
    }

    public StudentsTableModel setCap2(Integer cap2) {
        this.cap2 = cap2;
        return this;
    }

    public Integer getCap3() {
        return cap3;
    }

    public StudentsTableModel setCap3(Integer cap3) {
        this.cap3 = cap3;
        return this;
    }

    public Integer getBma1() {
        return bma1;
    }

    public StudentsTableModel setBma1(Integer bma1) {
        this.bma1 = bma1;
        return this;
    }

    public Integer getBma2() {
        return bma2;
    }

    public StudentsTableModel setBma2(Integer bma2) {
        this.bma2 = bma2;
        return this;
    }

    public Boolean getPro() {
        return pro;
    }

    public StudentsTableModel setPro(Boolean pro) {
        this.pro = pro;
        return this;
    }

    public Boolean getGeneral() {
        return general;
    }

    public StudentsTableModel setGeneral(Boolean general) {
        this.general = general;
        return this;
    }

    public Integer getTotalApril() {
        return totalApril;
    }

    public StudentsTableModel setTotalApril(Integer totalApril) {
        this.totalApril = totalApril;
        return this;
    }

    public Integer getSecondetechno() {
        return secondetechno;
    }

    public StudentsTableModel setSecondetechno(Integer secondetechno) {
        this.secondetechno = secondetechno;
        return this;
    }

    public Integer getPremieretechno() {
        return premieretechno;
    }

    public StudentsTableModel setPremieretechno(Integer premieretechno) {
        this.premieretechno = premieretechno;
        return this;
    }

    public Integer getTerminaletechno() {
        return terminaletechno;
    }

    public StudentsTableModel setTerminaletechno(Integer terminaletechno) {
        this.terminaletechno = terminaletechno;
        return this;
    }
}
