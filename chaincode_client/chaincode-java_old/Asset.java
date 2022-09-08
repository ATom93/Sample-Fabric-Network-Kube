/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class Asset {

    @Property()
    private final String ID;

    @Property()
    private String oggetto;

    @Property()
    private String descrizione;

    @Property()
    private int importoRDA;

    @Property()
    private String terminePresentazioni;

    @Property()
    private String tipoAggiudicazione;

    @Property()
    private String tipoOfferta;

    @Property()
    private String descrizioneCriterio;

    @Property()
    private int punteggioMassimo;


    @Property()
    private int punteggioTecnico;
    @Property()
    private int punteggioEconomico;

    private String[] states = {"IN_PREPARAZIONE", "IN_CORSO", "IN_VALUTAZIONE"};

    @Property()
    private String stato;


    public String getStato() {
        return stato;
    }

    public String getID() {
        return ID;
    }

    public String getOggetto() {
        return oggetto;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public int getImportoRDA() {
        return importoRDA;
    }

    public String getTerminePresentazioni() {
        return terminePresentazioni;
    }

    public String getTipoAggiudicazione() {
        return tipoAggiudicazione;
    }

    public String getTipoOfferta() {
        return tipoOfferta;
    }

    public String getDescrizioneCriterio() {
        return descrizioneCriterio;
    }

    public int getPunteggioMassimo() {
        return punteggioMassimo;
    }

    public int getPunteggioTecnico() {
        return punteggioTecnico;
    }

    public int getPunteggioEconomico() {
        return punteggioEconomico;
    }


    public void setStato(final String state) {
        this.stato = state;
    }

    public void setImporto(final int importo) {
        this.importoRDA = importo;
    }

    public Asset(@JsonProperty("ID") final String ID,
            @JsonProperty("stato") final String stato,
            @JsonProperty("oggetto") final String oggetto,
            @JsonProperty("descrizione") final String descrizione,
            @JsonProperty("importoRDA") final int importoRDA,
            @JsonProperty("terminePresentazioni") final String terminePresentazioni,
            @JsonProperty("tipoAggiudicazione") final String tipoAggiudicazione,
            @JsonProperty("tipoOfferta") final String tipoOfferta) {
        this.ID = ID;
        this.stato = stato;
        this.oggetto = oggetto;
        this.descrizione = descrizione;
        this.importoRDA = importoRDA;
        this.terminePresentazioni = terminePresentazioni;
        this.tipoAggiudicazione = tipoAggiudicazione;
        this.tipoOfferta = tipoOfferta;
    }

    /*
    public Asset(@JsonProperty("ID") final String ID, 
            @JsonProperty("stato") final String stato,
            @JsonProperty("oggetto") final String oggetto,
            @JsonProperty("descrizione") final String descrizione,
            @JsonProperty("importoRDA") final int importoRDA,
            @JsonProperty("terminePresentazioni") final String terminePresentazioni,
            @JsonProperty("tipoAggiudicazione") final String tipoAggiudicazione,
            @JsonProperty("tipoOfferta") final String tipoOfferta,
            @JsonProperty("descrizioneCriterio") final String descrizioneCriterio,
            @JsonProperty("punteggioMassimo") final int punteggioMassimo) {
        this.ID = ID;
        this.oggetto = oggetto;
        this.descrizione = descrizione;
        this.importoRDA = importoRDA;
        this.terminePresentazioni = terminePresentazioni;
        this.tipoAggiudicazione = tipoAggiudicazione;
        this.tipoOfferta = tipoOfferta;
        this.descrizioneCriterio = descrizioneCriterio;
        this.punteggioMassimo = punteggioMassimo;
    }
    */

    /*
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Asset other = (Asset) obj;

        return Objects.deepEquals(
                new String[] {getAssetID(), getColor(), getOwner()},
                new String[] {other.getAssetID(), other.getColor(), other.getOwner()})
                &&
                Objects.deepEquals(
                new int[] {getSize(), getAppraisedValue()},
                new int[] {other.getSize(), other.getAppraisedValue()});
    }
    */

    /*
    @Override
    public int hashCode() {
        return Objects.hash(
            getID(), 
            getOggetto(), 
            getImportoRDA());
    }
    */

    /*
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + 
        "@" + Integer.toHexString(hashCode()) + 
        " [ID=" + ID + 
        ", oggetto=" + oggetto + 
        ", descrizione=" + descrizione + 
        ", importo RDA=" + importoRDA + 
        ", termine presentazioni=" + terminePresentazioni.toString() +
        ", tipoAggiudicazione=" + tipoAggiudicazione +
        ", tipoOfferta=" + tipoOfferta +
        "]";
    }
    */
}