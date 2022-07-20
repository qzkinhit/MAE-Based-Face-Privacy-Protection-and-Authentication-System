# database.py
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# 配置数据库信息
username = "root"   # 数据库连接用户名
pwd = "123456"      # 数据库连接密码
DBname = "maes"     # 数据库名称

SQLALCHEMY_DATABASE_URL = f'mysql+mysqlconnector://{username}:{pwd}@localhost:3306/{DBname}?charset=utf8&auth_plugin' \
                          f'=mysql_native_password '
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, pool_pre_ping=True
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()
