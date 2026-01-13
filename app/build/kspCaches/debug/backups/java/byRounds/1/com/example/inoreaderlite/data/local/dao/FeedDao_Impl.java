package com.example.inoreaderlite.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.inoreaderlite.data.local.entity.ArticleEntity;
import com.example.inoreaderlite.data.local.entity.SourceEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FeedDao_Impl implements FeedDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ArticleEntity> __insertionAdapterOfArticleEntity;

  private final EntityInsertionAdapter<SourceEntity> __insertionAdapterOfSourceEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAllArticles;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSource;

  public FeedDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfArticleEntity = new EntityInsertionAdapter<ArticleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `articles` (`link`,`title`,`description`,`pubDate`,`sourceUrl`,`imageUrl`,`isRead`,`isSaved`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArticleEntity entity) {
        statement.bindString(1, entity.getLink());
        statement.bindString(2, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        statement.bindLong(4, entity.getPubDate());
        statement.bindString(5, entity.getSourceUrl());
        if (entity.getImageUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getImageUrl());
        }
        final int _tmp = entity.isRead() ? 1 : 0;
        statement.bindLong(7, _tmp);
        final int _tmp_1 = entity.isSaved() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
      }
    };
    this.__insertionAdapterOfSourceEntity = new EntityInsertionAdapter<SourceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sources` (`url`,`title`,`iconUrl`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SourceEntity entity) {
        statement.bindString(1, entity.getUrl());
        statement.bindString(2, entity.getTitle());
        if (entity.getIconUrl() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getIconUrl());
        }
      }
    };
    this.__preparedStmtOfClearAllArticles = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM articles";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteSource = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sources WHERE url = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertArticles(final List<ArticleEntity> articles,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfArticleEntity.insert(articles);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSource(final SourceEntity source,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSourceEntity.insert(source);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllArticles(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllArticles.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAllArticles.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSource(final String url, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSource.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, url);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteSource.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ArticleEntity>> getAllArticles() {
    final String _sql = "SELECT * FROM articles ORDER BY pubDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"articles"}, new Callable<List<ArticleEntity>>() {
      @Override
      @NonNull
      public List<ArticleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLink = CursorUtil.getColumnIndexOrThrow(_cursor, "link");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPubDate = CursorUtil.getColumnIndexOrThrow(_cursor, "pubDate");
          final int _cursorIndexOfSourceUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceUrl");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsSaved = CursorUtil.getColumnIndexOrThrow(_cursor, "isSaved");
          final List<ArticleEntity> _result = new ArrayList<ArticleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ArticleEntity _item;
            final String _tmpLink;
            _tmpLink = _cursor.getString(_cursorIndexOfLink);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpPubDate;
            _tmpPubDate = _cursor.getLong(_cursorIndexOfPubDate);
            final String _tmpSourceUrl;
            _tmpSourceUrl = _cursor.getString(_cursorIndexOfSourceUrl);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsSaved;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSaved);
            _tmpIsSaved = _tmp_1 != 0;
            _item = new ArticleEntity(_tmpLink,_tmpTitle,_tmpDescription,_tmpPubDate,_tmpSourceUrl,_tmpImageUrl,_tmpIsRead,_tmpIsSaved);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ArticleEntity>> getArticlesBySource(final String sourceUrl) {
    final String _sql = "SELECT * FROM articles WHERE sourceUrl = ? ORDER BY pubDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceUrl);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"articles"}, new Callable<List<ArticleEntity>>() {
      @Override
      @NonNull
      public List<ArticleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLink = CursorUtil.getColumnIndexOrThrow(_cursor, "link");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPubDate = CursorUtil.getColumnIndexOrThrow(_cursor, "pubDate");
          final int _cursorIndexOfSourceUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceUrl");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsSaved = CursorUtil.getColumnIndexOrThrow(_cursor, "isSaved");
          final List<ArticleEntity> _result = new ArrayList<ArticleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ArticleEntity _item;
            final String _tmpLink;
            _tmpLink = _cursor.getString(_cursorIndexOfLink);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpPubDate;
            _tmpPubDate = _cursor.getLong(_cursorIndexOfPubDate);
            final String _tmpSourceUrl;
            _tmpSourceUrl = _cursor.getString(_cursorIndexOfSourceUrl);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsSaved;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSaved);
            _tmpIsSaved = _tmp_1 != 0;
            _item = new ArticleEntity(_tmpLink,_tmpTitle,_tmpDescription,_tmpPubDate,_tmpSourceUrl,_tmpImageUrl,_tmpIsRead,_tmpIsSaved);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<SourceEntity>> getAllSources() {
    final String _sql = "SELECT * FROM sources";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sources"}, new Callable<List<SourceEntity>>() {
      @Override
      @NonNull
      public List<SourceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfIconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "iconUrl");
          final List<SourceEntity> _result = new ArrayList<SourceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SourceEntity _item;
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpIconUrl;
            if (_cursor.isNull(_cursorIndexOfIconUrl)) {
              _tmpIconUrl = null;
            } else {
              _tmpIconUrl = _cursor.getString(_cursorIndexOfIconUrl);
            }
            _item = new SourceEntity(_tmpUrl,_tmpTitle,_tmpIconUrl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllSourcesList(final Continuation<? super List<SourceEntity>> $completion) {
    final String _sql = "SELECT * FROM sources";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SourceEntity>>() {
      @Override
      @NonNull
      public List<SourceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfIconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "iconUrl");
          final List<SourceEntity> _result = new ArrayList<SourceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SourceEntity _item;
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpIconUrl;
            if (_cursor.isNull(_cursorIndexOfIconUrl)) {
              _tmpIconUrl = null;
            } else {
              _tmpIconUrl = _cursor.getString(_cursorIndexOfIconUrl);
            }
            _item = new SourceEntity(_tmpUrl,_tmpTitle,_tmpIconUrl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
