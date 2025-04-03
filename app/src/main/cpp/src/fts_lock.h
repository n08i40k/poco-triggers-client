//
// Created by n08i40k on 03.04.2025.
//

#ifndef FTS_LOCK_H
#define FTS_LOCK_H

class fts_lock
{
    int fd_{};

public:
    fts_lock();

    ~fts_lock();

    [[nodiscard]]
    int
    fd() const
    {
        return fd_;
    }
};

#endif // FTS_LOCK_H
