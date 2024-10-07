package ai.rag.elastic.chat;

public class TradeName {
    String tradeNameId;
    String tradeNameStr;

    public TradeName(String tradeNameId, String tradeNameStr) {
        this.tradeNameId = tradeNameId;
        this.tradeNameStr = tradeNameStr;
    }

    public String getTradeNameId() {
        return tradeNameId;
    }

    public void setTradeNameId(String tradeNameId) {
        this.tradeNameId = tradeNameId;
    }

    public String getTradeNameStr() {
        return tradeNameStr;
    }

    public void setTradeNameStr(String tradeNameStr) {
        this.tradeNameStr = tradeNameStr;
    }
}
