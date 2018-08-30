package com.shuyun.datasync.core.template.strategy;

import com.shuyun.datasync.domain.ColumnMapping;
import com.shuyun.datasync.domain.TaskConfig;
import com.shuyun.datasync.utils.ZKLock;
import com.shuyun.datasync.utils.ZKUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiawei.guo on 2018/8/21.
 */
public class CoverSyncStrategySerial extends CoverSyncStrategy {
    private static Logger logger = Logger.getLogger(CoverSyncStrategySerial.class);

    public static void handle(List<String> tables,final TaskConfig tc) {
        SparkSession spark = createSparkSession(tc);

        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());
        if(MapUtils.isNotEmpty(tc.getSparkConfigProperties())) {
            for(String key : tc.getSparkConfigProperties().keySet()) {
                sc.getConf().set(key, tc.getSparkConfigProperties().get(key));
            }
        }

        Broadcast<TaskConfig> taskConfigBroad = sc.broadcast(tc);

        StructType schema = createSchema(tc);

        for(String table : tables) {
            ZKLock lock = ZKUtil.lock(table);

            JavaRDD<Row> dataRDD = createHbaseRDD(sc, table, taskConfigBroad);

            coverData(spark, dataRDD, schema, tc, table);

            updateTableStatus(table);
            lock.release();
        }
        spark.close();
        spark.stop();
    }


}
