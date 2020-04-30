import pyspark
from pyspark.sql import functions as F, types as T

class data_quality(object):
    _j_dq_client = None
    @classmethod
    def _set_jvm_client(cls, client):
        cls._j_dq_client = client
    
    def runDQ(self, df):
        msg = self._j_dq_client.getDataQualityOfDF(df)
        return msg
    
    def __init__(self, jvm):
        self._set_jvm_client(jvm.au.com.telstra.oan.dataCuration.DataQualityWrapper)

if __name__ == '__main__':
    spark = pyspark.sql.SparkSession \
        .builder \
        .config("spark.jars", "./dqwrapper.jar") \
        .enableHiveSupport() \
        .getOrCreate()       


    schema = T.StructType([
        T.StructField("id", T.LongType()),
        T.StructField("productName", T.StringType()),
        T.StructField("description", T.StringType()),
        T.StructField("priority", T.StringType()),
        T.StructField("numViews", T.LongType())
    ])
    data = [
        [1, "Thingy A", "awesome thing.", "high", 0],
        [2, "Thingy B", "available at http://thingb.com", None, 0],
        [3, None, None, "low", 5],
        [4, "Thingy D", "checkout https://thingd.ca", "low", 10],
        [5, "Thingy E", None, "high", 12]
    ]
    df = spark.createDataFrame(data, schema=schema)
    df.show()
    dq = data_quality(spark.sparkContext._jvm)
    msg = dq.runDQ(df._jdf)
    print("DQ message: " + msg)
