package com.higgschain.trust.slave.dao.mysql.pack;

import com.higgschain.trust.slave.BaseTest;
import com.higgschain.trust.slave.dao.po.pack.PackagePO;
import com.higgschain.trust.slave.model.enums.biz.PackageStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The type Package dao test.
 */
/*
 *
 * @desc
 * @author tangfashuang
 * @date 2018/4/14
 *
 */
public class PackageDaoTest extends BaseTest {

    @Autowired
    private PackageDao packageDao;

    private PackagePO packagePO = new PackagePO();

    /**
     * Sets up.
     *
     * @throws Exception the exception
     */
    @BeforeMethod public void setUp() throws Exception {
        packagePO.setStatus(PackageStatusEnum.WAIT_PERSIST_CONSENSUS.getCode());
        packagePO.setPackageTime(System.currentTimeMillis());
        packagePO.setHeight(4L);
    }

    /**
     * Save.
     */
    @Test
    public  void save() {
        packageDao.add(packagePO);

        PackagePO po = packageDao.queryByHeight(packagePO.getHeight());

        Assert.assertEquals(po.getHeight(), packagePO.getHeight());
        Assert.assertEquals(po.getPackageTime(), packagePO.getPackageTime());
        Assert.assertEquals(po.getStatus(), packagePO.getStatus());
    }

    /**
     * Query by height.
     */
    @Test public void queryByHeight() {
        PackagePO po = packageDao.queryByHeight(packagePO.getHeight());

        Assert.assertEquals(po.getHeight(), packagePO.getHeight());
        Assert.assertEquals(po.getPackageTime(), packagePO.getPackageTime());
        Assert.assertEquals(po.getStatus(), packagePO.getStatus());
    }

    /**
     * Query by height for update.
     */
    @Test public void queryByHeightForUpdate() {
    }

    /**
     * Update status.
     */
    @Test public void updateStatus() {
        packageDao.updateStatus(2L, PackageStatusEnum.RECEIVED.getCode(), PackageStatusEnum.WAIT_PERSIST_CONSENSUS.getCode());

        PackagePO po = packageDao.queryByHeightForUpdate(2L);

        Assert.assertEquals(po.getStatus(), PackageStatusEnum.WAIT_PERSIST_CONSENSUS.getCode());

    }

    /**
     * Gets max height.
     */
    @Test public void getMaxHeight() {
        Long max = packageDao.getMaxHeight();
        Assert.assertEquals(4L, max.longValue());
    }

    /**
     * Query by status.
     */
    @Test public void queryByStatus() {
        List<PackagePO> packagePOList = packageDao.queryByStatus(PackageStatusEnum.WAIT_PERSIST_CONSENSUS.getCode());
        packagePOList.forEach(packagePO1 ->  {
            System.out.println(packagePO1);
        });
        Assert.assertEquals(2, packagePOList.size());
    }

    /**
     * Count with status.
     */
    @Test public void countWithStatus() {
        Set<String> statusSet = new HashSet<>();
//        statusSet.add(PackageStatusEnum.SUBMIT_CONSENSUS_SUCCESS.getCode());
        statusSet.add(PackageStatusEnum.WAIT_PERSIST_CONSENSUS.getCode());

        long count = packageDao.countWithStatus(statusSet, 2L);
        Assert.assertEquals(1, count);
    }
}