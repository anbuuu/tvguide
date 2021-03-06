/*
 * Copyright (C) 2011,2012  Southern Storm Software, Pty Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef _TVPROGRAMME_H
#define _TVPROGRAMME_H

#include <QtCore/qstring.h>
#include <QtCore/qstringlist.h>
#include <QtCore/qdatetime.h>
#include <QtCore/qxmlstream.h>
#include <QtCore/qmap.h>
#include <QtCore/qset.h>
#include <QtGui/qcolor.h>
#include <QtGui/qtextcursor.h>
#include <QtGui/qtextdocument.h>
#include "tvbookmark.h"
#include "tvdatetime.h"

class TvChannel;

class TvProgramme
{
public:
    explicit TvProgramme(TvChannel *channel);
    ~TvProgramme();

    TvChannel *channel() const { return m_channel; }

    TvDateTime start() const { return m_start; }
    void setStart(const QDateTime &start) { m_start = start; }

    TvDateTime stop() const { return m_stop; }
    void setStop(const QDateTime &stop) { m_stop = stop; }

    int secondsLength() const;

    QString title() const { return m_title; }
    void setTitle(const QString &title)
        { m_title = title; m_indexTitle = title.toLower(); }

    QString indexTitle() const { return m_indexTitle; }

    QString subTitle() const { return m_subTitle; }
    QString description() const { return m_description; }
    QString date() const { return m_date; }
    int year() const;
    QStringList directors() const { return m_directors; }
    QStringList actors() const { return m_actors; }
    QStringList presenters() const { return m_presenters; }
    QStringList categories() const { return m_categories; }
    QString rating() const { return m_rating; }
    QString starRating() const { return m_starRating; }
    QString episodeNumber() const { return m_episodeNumber; }
    int season() const { return m_season; }
    QString language() const { return m_language; }
    QString originalLanguage() const { return m_originalLanguage; }
    QString country() const { return m_country; }
    QString aspectRatio() const { return m_aspectRatio; }
    bool isPremiere() const { return m_isPremiere; }
    bool isRepeat() const { return m_isRepeat; }
    bool isMovie() const { return m_isMovie; }

    void load(QXmlStreamReader *reader);

    TvBookmark *bookmark() const { return m_bookmark; }
    TvBookmark::Match match() const { return m_match; }
    void setBookmark(TvBookmark *bookmark, TvBookmark::Match match);

    QTextDocument *shortDescriptionDocument() const;

    QString longDescription() const;

    enum {
        Write_Actor         = 0x00000001,
        Write_Category      = 0x00000002,
        Write_EpisodeName   = 0x00000004,
        Write_OtherShowings = 0x00000008,
        Write_Date          = 0x00000010,
        Write_StarRating    = 0x00000020,
        Write_Description   = 0x00010000,
        Write_Continued     = 0x00020000,
        Write_Short         = 0x0000FFFF,
        Write_All           = 0x7FFFFFFF
    };

    // Writes the short form of the programme description
    // to a QTextDocument object via a QTextCursor.
    void writeShortDescription(QTextCursor *cursor, int options) const;

    bool isSuppressed() const { return m_suppressed; }
    void setSuppressed(bool value) { m_suppressed = value; }

    void refreshBookmark();

    enum SearchType
    {
        SearchTitle,
        SearchEpisodeName,
        SearchDescription,
        SearchCredits,
        SearchCategories,
        SearchAll
    };

    bool containsSearchString(const QString &str, SearchType type) const;
    bool containsSearchString(const QStringList &list, SearchType type) const;

    void updateCategorySet(QSet<QString> &set) const;
    void updateCreditSet(QSet<QString> &set) const;

    bool overlapsWith(const TvProgramme *prog) const;

private:
    TvChannel *m_channel;
    TvDateTime m_start;
    TvDateTime m_stop;
    QString m_title;
    QString m_indexTitle;
    QString m_subTitle;
    QString m_description;
    QString m_date;
    QStringList m_directors;
    QStringList m_actors;
    QStringList m_presenters;
    QMap<QString, QStringList> m_otherCredits;
    QStringList m_categories;
    QString m_rating;
    QString m_starRating;
    QString m_episodeNumber;
    int m_season;
    QString m_language;
    QString m_originalLanguage;
    QString m_country;
    QString m_aspectRatio;
    bool m_isPremiere;
    bool m_isRepeat;
    bool m_isMovie;
    bool m_suppressed;
    mutable QTextDocument *m_shortDescriptionDocument;
    mutable QString m_longDescription;
    TvBookmark *m_bookmark;
    TvBookmark::Match m_match;
    TvProgramme *m_prev;
    TvProgramme *m_next;

    friend class TvChannel;
    friend class TvBookmark;

    void clearBookmarkMatch();
    void markDirty();

    TvBookmark::Match displayMatch() const;

    QString formatOtherShowings() const;

    void addOtherCredit(const QString &type, const QString &name);

    inline bool containsSearch(const QString &str, const QString &within) const
    {
        return within.indexOf(str, 0, Qt::CaseInsensitive) >= 0;
    }

    bool containsSearch(const QString &str, const QStringList &within) const;

    static void updateSet(QSet<QString> &set, const QStringList &list);
};

#endif
