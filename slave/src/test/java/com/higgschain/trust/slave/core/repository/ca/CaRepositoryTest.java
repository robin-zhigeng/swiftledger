package com.higgschain.trust.slave.core.repository.ca;

import com.higgschain.trust.slave.BaseTest;
import com.higgschain.trust.slave.api.enums.VersionEnum;
import com.higgschain.trust.slave.model.bo.ca.Ca;
import com.higgschain.trust.tester.dbunit.DataBaseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * The type Ca repository test.
 */
public class CaRepositoryTest extends BaseTest {

    /**
     * The Data base manager.
     */
    DataBaseManager dataBaseManager = new DataBaseManager();

    @Autowired private CaRepository caRepository;

    /**
     * The Url.
     */
    String url =
        "jdbc:mysql://localhost:3306/trust?user=root&password=root&useUnicode=true&characterEncoding=UTF8&allowMultiQueries=true&useAffectedRows=true";
    /**
     * The Sql.
     */
    String sql = "truncate table ca;";

    /**
     * Test insert ca.
     *
     * @throws Exception the exception
     */
    @Test public void testInsertCa() throws Exception {
        Ca ca = new Ca();
        ca.setPeriod(new Date());
        ca.setPubKey("123");
        ca.setUsage("consensus");
        ca.setUser("wqz");
        ca.setValid(true);
        ca.setVersion(VersionEnum.V1.getCode());
        caRepository.insertCa(ca);
    }

    /**
     * Test update ca.
     *
     * @throws Exception the exception
     */
    @Test public void testUpdateCa() throws Exception {
        Ca ca = new Ca();
        ca.setUser("wqz");
        ca.setPeriod(new Date());
        ca.setPubKey("456");
        caRepository.updateCa(ca);
    }

    /**
     * Test get ca.
     *
     * @throws Exception the exception
     */
    @Test public void testGetCa() throws Exception {
        Ca ca = caRepository.getCaForBiz("TRUST-TEST0");
        System.out.println(ca.toString());
    }

    /**
     * Test get all ca.
     *
     * @throws Exception the exception
     */
    @Test public void testGetAllCa() throws Exception {
        List list = caRepository.getAllCa();
        System.out.println(list.toString());
    }

    /**
     * Test batch insert.
     *
     * @throws Exception the exception
     */
    @Test public void testBatchInsert() throws Exception {
        List list =new LinkedList();
        for (int i =10;i<11;i++){
            Ca ca = new Ca();
            ca.setPeriod(new Date());
            ca.setPubKey("test"+i);
            ca.setUsage("consensus");
            ca.setUser("lf");
            ca.setValid(true);
            ca.setVersion(VersionEnum.V1.getCode());
            list.add(ca);
        }
        caRepository.batchInsert(list);
        System.out.println("结束");
    }

    /**
     * Test batch update.
     *
     * @throws Exception the exception
     */
    @Test public void testBatchUpdate() throws Exception {
        List list =new LinkedList();
        for (int i =0;i<1;i++){
            Ca ca = new Ca();
            ca.setPeriod(new Date());
            ca.setPubKey("test"+1000);
            ca.setUser("wqz");
            list.add(ca);
        }
        Ca ca = new Ca();
        ca.setPeriod(new Date());
        ca.setPubKey("test"+10000);
        ca.setUser("lf");
        list.add(ca);
        caRepository.batchUpdate(list);
        System.out.println("结束");
    }
}