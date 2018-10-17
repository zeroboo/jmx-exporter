package com.nhnent.ongame.management;

/**
 * Management API of Xito
 */
public interface OngameManagementAPIMBean {

    /**
     * Get Channel Info
     * @return Channel info with JSon format
     */
    String[] getChannelInfo();

    String[] getChannelStatus();


    String getRoomInfoList(String channelId);

    String getUserList(String channelId);

    /**
     * @param onGameId : ex nhntest001
     * @param reason   - kicked reason to notify user.
     * @return
     */
    boolean kickOutPlayer(String onGameId, String adminId, String reason);

    boolean updateVipInfos();

    boolean updateVipBenefitLevelInfos();

    boolean updateVipExchangeInfos();

    boolean updateVipDataInfos() ;

    boolean changeDataEventLoginBonus();

    boolean updateTanCards(String adminId, String jsonCards);


    boolean updateGachaInfos();

    /**
     * Make changes to mission's info stored in data tables: poker_mission & poker_mission_level
     * @param adminId: Who make changes
     * @param reason: Reason of changes
     *
     */
    boolean updateMissionInfo(String adminId, String reason);

}
