//
// Created by n08i40k on 03.04.2025.
//

#ifndef SERVER_H
#define SERVER_H

#include <functional>
#include <linux/in.h>
#include <sys/endian.h>

struct trigger_settings {
private:
    std::uint16_t index_{};
    std::uint16_t enabled_{};

    std::int32_t x_{};
    std::int32_t y_{};

public:
    [[nodiscard]]
    std::uint8_t
    index() const
    {
        return static_cast<std::uint8_t>(index_);
    }

    [[nodiscard]]
    std::uint8_t
    inv_index() const
    {
        return index_ == 0 ? 1 : 0;
    }

    [[nodiscard]]
    bool
    enabled() const
    {
        return enabled_ > 0;
    }

    [[nodiscard]]
    std::int32_t
    x() const
    {
        return x_;
    }

    [[nodiscard]]
    std::int32_t
    y() const
    {
        return y_;
    }
};

struct client_message {
    trigger_settings upper;
    trigger_settings lower;
};

class server
{
    sockaddr_in address_{};
    int         fd_{ 0 };

public:
    explicit server(std::uint16_t port = 5555);

    ~server();

    void
    work_thread(const std::function<void(client_message&)>& on_message);

    [[nodiscard]]
    int
    fd() const
    {
        return fd_;
    }
};

#endif // SERVER_H
