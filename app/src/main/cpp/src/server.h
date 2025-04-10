//
// Created by n08i40k on 03.04.2025.
//

#ifndef SERVER_H
#define SERVER_H

#include <functional>
#include <linux/in.h>
#include <sys/endian.h>
#include <thread>

struct trigger_settings {
    bool enabled;

    std::int32_t x;
    std::int32_t y;
};

struct client_message {
    trigger_settings upper;
    trigger_settings lower;
};

class server
{
    sockaddr_in address_{};
    int         fd_{ 0 };

    void
    work_thread(const std::function<void(client_message&)>& on_message);

public:
    explicit server(std::uint16_t port = 5555);

    ~server();

    [[nodiscard]]
    std::thread
    start_thread(const std::function<void(client_message&)>& on_message);

    [[nodiscard]]
    int
    fd() const
    {
        return fd_;
    }
};

#endif // SERVER_H
