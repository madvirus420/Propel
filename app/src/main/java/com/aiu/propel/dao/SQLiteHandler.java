package com.aiu.propel.dao;

import com.aiu.propel.util.RpeVO;

import java.sql.SQLException;
import java.util.List;


public interface SQLiteHandler {

    public long insertSessions(Long rpe);

    public List<RpeVO> getPastRecord();

    public SQLiteHandlerImpl open() throws SQLException;

    public void close() throws SQLException;
}
