package commands

import com.alibaba.fastjson.JSON
import com.higgschain.trust.common.dao.RocksDBHelper
import lombok.extern.slf4j.Slf4j
import org.crsh.cli.Argument
import org.crsh.cli.Command
import org.crsh.cli.Required
import org.crsh.cli.Usage
import org.crsh.command.InvocationContext
import org.springframework.beans.factory.BeanFactory

/**
 * @desc rocksdb tools
 * @author liuyu
 * @date 2018/09/13 16:16
 */

/**
 *  The type Rocksdb.
 */
@Slf4j
@Usage("rocksdb tools")
class rocksdb {

    @Usage('show all table names')
    @Command
    def showTables(InvocationContext context) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def helper = beans.getBean(RocksDBHelper.class)
        def result = helper.showTables()
        if (result) {
            out.println("$result")
        } else {
            out.println("is empty")
        }
    }

    @Usage('query by key')
    @Command
    def queryByKey(InvocationContext context,
                   @Usage("tableName") @Required @Argument String tableName,
                   @Usage("keyName") @Required @Argument String keyName) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def helper = beans.getBean(RocksDBHelper.class)
        def result = helper.queryByKey(tableName, keyName)
        if (result) {
            def json = JSON.toJSONString(result)
            out.println("$json")
        } else {
            out.println("is empty")
        }
    }

    @Usage('query by prefix and limit size')
    @Command
    def queryByPrefix(InvocationContext context,
                      @Usage("tableName") @Required @Argument String tableName,
                      @Usage("prefix") @Required @Argument String prefix,
                      @Usage("limit") @Argument int limit
    ) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def helper = beans.getBean(RocksDBHelper.class)
        def result = helper.queryByPrefix(tableName, prefix, limit)
        if (result) {
            def json = JSON.toJSONString(result)
            out.println("$json")
        } else {
            out.println("is empty")
        }
    }

    @Usage('query by count and order')
    @Command
    def queryByCount(InvocationContext context,
                     @Usage("tableName") @Required @Argument String tableName,
                     @Usage("count") @Required @Argument int count,
                     @Usage("order 0-DESC 1-ASC") @Required @Argument int order
    ) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def helper = beans.getBean(RocksDBHelper.class)
        def result = helper.queryByCount(tableName, count, order)
        if (result) {
            def json = JSON.toJSONString(result)
            out.println("$json")
        } else {
            out.println("is empty")
        }
    }


    @Usage('count by')
    @Command
    def count(InvocationContext context,
              @Usage("tableName") @Required @Argument String tableName,
              @Usage("prefix") @Argument String prefix
    ) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def helper = beans.getBean(RocksDBHelper.class)
        def result = helper.countBy(tableName,prefix)
        if (result) {
            out.println("$result")
        } else {
            out.println("is empty")
        }
    }


    @Usage('clear tables')
    @Command
    def clear(InvocationContext context,
              @Usage("to clear table names,multiple comma-connected") @Required @Argument String tableNames) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def helper = beans.getBean(RocksDBHelper.class)
        def result = helper.clear(tableNames.split(","))
        if (result) {
            out.println("clear is success")
        } else {
            out.println("clear is fail")
        }
    }


    @Usage('clear all tables allow ignored')
    @Command
    def clearAll(InvocationContext context,
                 @Usage("ignored table names,multiple comma-connected") @Argument String ignoreTables) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def helper = beans.getBean(RocksDBHelper.class)
        def result = helper.clearAll(ignoreTables == null ? null : ignoreTables.split(","))
        if (result) {
            out.println("clear all is success")
        } else {
            out.println("clear all is fail")
        }
    }

    @Usage('put the value')
    @Command
    def put(InvocationContext context,
            @Usage("tableName") @Required @Argument String tableName,
            @Usage("key") @Required @Argument String keyName,
            @Usage("value") @Required @Argument String jsonStr, @Usage("the class of value")@Required @Argument String clazzName) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def helper = beans.getBean(RocksDBHelper.class)
        def value = JSON.parseObject(jsonStr, Class.forName(clazzName))
        helper.put(tableName, keyName, value)
    }

}
