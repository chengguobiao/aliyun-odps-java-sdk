/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.odps.tunnel.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.aliyun.odps.TableSchema;
import com.aliyun.odps.data.Record;
import com.aliyun.odps.data.RecordPack;
import com.aliyun.odps.data.RecordReader;

/**
 * 用 Protobuf 序列化存储的 {@link RecordPack}
 * 和 TableTunnel 共同使用
 * 比直接使用 List<Record> 更加节省内存
 */
public class ProtobufRecordPack extends RecordPack {

  private ProtobufRecordStreamWriter writer;
  private ByteArrayOutputStream byteos;
  private long count = 0;
  private TableSchema schema;

  /**
   * 新建一个ProtobufRecordPack
   *
   * @param schema
   * @throws IOException
   */
  public ProtobufRecordPack(TableSchema schema) throws IOException {
    this(schema, new Checksum());
  }

  /**
   * 新建一个 ProtobufRecordPack，用对应的 CheckSum 初始化
   *
   * @param schema
   * @param checkSum
   * @throws IOException
   */
  public ProtobufRecordPack(TableSchema schema, Checksum checkSum) throws IOException {
    this(schema, checkSum, 0);
  }

  /**
   * 新建一个 ProtobufRecordPack，用对应的 CheckSum 初始化, 并且预设流 buffer 大小为 capacity
   *
   * @param schema
   * @param checkSum
   * @param capacity
   * @throws IOException
   */
  public ProtobufRecordPack(TableSchema schema, Checksum checkSum, int capacity)
      throws IOException {
    if (capacity == 0) {
      byteos = new ByteArrayOutputStream();
    } else {
      byteos = new ByteArrayOutputStream(capacity);
    }
    this.schema = schema;
    writer = new ProtobufRecordStreamWriter(schema, byteos);
    if (checkSum != null) {
      writer.setCheckSum(checkSum);
    }
  }

  @Override
  public void append(Record a) throws IOException {
    writer.write(a);
    ++count;
  }


  /**
   * 获取 RecordReader 对象
   * ProtobufRecordPack 不支持改方法
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public RecordReader getRecordReader() throws IOException {
    throw new UnsupportedOperationException("PBPack does not supported Read.");
  }

  ByteArrayOutputStream getProtobufStream() throws IOException {
    writer.flush();
    return byteos;
  }

  /**
   * 获取输出数据序列化后的总大小
   *
   * @return
   * @throws IOException
   */
  public long getTotalBytes() throws IOException {
    return getProtobufStream().size();
  }

  /**
   * 获取 Record 的 CheckSum
   */
  public Checksum getCheckSum() {
    return writer.getCheckSum();
  }

  /**
   * 清空 RecordPack
   */
  public void reset() throws IOException {
    if (byteos != null) {
      byteos.reset();
    }
    count = 0;
    this.writer = new ProtobufRecordStreamWriter(schema, byteos);
  }


  /**
   * 清空 RecordPack
   *
   * @param checksum
   *     初始化 checksum
   */
  public void reset(Checksum checksum) throws IOException {
    if (byteos != null) {
      byteos.reset();
    }
    count = 0;
    this.writer = new ProtobufRecordStreamWriter(schema, byteos);

    if (checksum != null) {
      this.writer.setCheckSum(checksum);
    }
  }

  long getSize() {
    return count;
  }
}